package com.indevelopment.sock.billing;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles all the interactions with Play Store (via Billing library), maintains connection to
 * it through BillingClient and caches temporary states/data if needed
 */
public class BillingManager implements PurchasesUpdatedListener {
    private static final String TAG = "BillingManager";

    /**
     * A reference to BillingClient
     **/
    private BillingClient mBillingClient;

    private final Activity mActivity;

    private List<SkuDetails> mSkuDetails = new ArrayList<>();

    public static final String ITEM_SKU = "indevelopment.sock.premium";

    /* BASE_64_ENCODED_PUBLIC_KEY should be YOUR APPLICATION'S PUBLIC KEY
     * (that you got from the Google Play developer console). This is not your
     * developer public key, it's the *app-specific* public key.
     *
     * Instead of just storing the entire literal string here embedded in the
     * program,  construct the key at runtime from pieces or
     * use bit manipulation (for example, XOR with some other string) to hide
     * the actual key.  The key itself is not secret information, but we don't
     * want to make it easy for an attacker to replace the public key with one
     * of their own and then fake messages from the server.
     */
    private static final String BASE_64_ENCODED_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0fgh18v1nnfnBZJ6BuMknbJrwBKvlVTvqvZBm3FiFchk1Au8yVtunLnwbCNs6ObP0qJsoBm9tYY3FeAQz3y5RafITFvHp5bjEUw1aRTJqiRc6U+r3wBL5U4oxlkFfziClzCSfLTWuVEn9ESLc7QW7E4TzM9xT+DvQVgkwkfTPHYb0AllXg4q4Y/SasCrEzYuwQ3C6DyF51ESk2tgjuHLKThTZERe/ls6qbeiSCa1Ywn4f/q/Z13uWpEvk0WHLincH0RkItmmTBJp4AKUMs/ZpsmyYMktlNcpE+E66n9xYCQKQegTU9tMk9Jkq3a23wBpBnimXgRiosfgQA/ReJnShwIDAQAB";

    public interface BillingUpdatesListener {
        void onPurchasesUpdated(Purchase purchase);
    }

    /**
     * True if billing service is connected now.
     */
    private boolean mIsServiceConnected;

    private final BillingUpdatesListener mBillingUpdatesListener;

    public BillingManager(final Activity activity, final BillingUpdatesListener billingUpdatesListener) {
        Log.d(TAG, "Creating Billing client.");
        mActivity = activity;
        mBillingUpdatesListener = billingUpdatesListener;
        mBillingClient = BillingClient.newBuilder(mActivity).setListener(this).enablePendingPurchases().build();

        Log.d(TAG, "Starting setup.");

        // Start setup. This is asynchronous and the specified listener will be called
        // once setup completes.
        // It also starts to report all the new purchases through onPurchasesUpdated() callback.
        startServiceConnection(new Runnable() {
            @Override
            public void run() {
                // IAB is fully set up. Now, let's get an inventory of stuff we own
                // and query what we sells.
                Log.d(TAG, "Setup successful. Querying inventory and SKU Details.");
                querySkuDetails(new String[]{ITEM_SKU});
                queryPurchases();
            }
        });
    }

    private void querySkuDetails(final String[] skus) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                List<String> skusList = Arrays.asList(skus);

                Log.d(TAG, "Successfully converting SKUs array to SKUs list with size: " + skusList.size());

                SkuDetailsParams skuDetailsParams = SkuDetailsParams.newBuilder()
                        .setSkusList(skusList)
                        .setType(BillingClient.SkuType.INAPP)
                        .build();
                final long time = System.currentTimeMillis();
                mBillingClient.querySkuDetailsAsync(skuDetailsParams, new SkuDetailsResponseListener() {
                    @Override
                    public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetailsList) {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                            mSkuDetails.addAll(skuDetailsList);
                            Log.d(TAG, "Querying SKU details elapsed time: " + (System.currentTimeMillis() - time)
                                    + "ms with List size: " + mSkuDetails.size());
                        }
                    }
                });
            }
        };

        executeServiceRequest(runnable);
    }

    private void queryPurchases() {
        Runnable queryToExecute = new Runnable() {
            @Override
            public void run() {
                long time = System.currentTimeMillis();
                Purchase.PurchasesResult purchasesResult = mBillingClient.queryPurchases(BillingClient.SkuType.INAPP);
                Log.i(TAG, "Querying purchases elapsed time: " + (System.currentTimeMillis() - time)
                        + "ms");
                onQueryPurchasesFinished(purchasesResult);
            }
        };

        executeServiceRequest(queryToExecute);
    }

    private void onQueryPurchasesFinished(Purchase.PurchasesResult purchasesResult) {
        // Have we been disposed of in the meantime? If so, or bad result code, then quit
        if (mBillingClient == null || purchasesResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            Log.w(TAG, "Billing client was null or result code (" + purchasesResult.getResponseCode()
                    + ") was bad – quitting");
            return;
        }

        Log.d(TAG, "Query inventory was successful.");

        // Update the UI and purchases inventory with new list of purchases
        BillingResult billingResult = BillingResult.newBuilder()
                .setResponseCode(purchasesResult.getResponseCode())
                .build();
        onPurchasesUpdated(billingResult, purchasesResult.getPurchasesList());
    }

    /**
     * Start a purchase flow
     */
    public void initiatePurchaseFlow(int index) {
        initiatePurchaseFlow(mSkuDetails.get(index), null);
    }

    /**
     * Start a purchase or subscription replace flow
     */
    public void initiatePurchaseFlow(final SkuDetails skuDetails, final String oldSku) {
        Runnable purchaseFlowRequest = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Launching in-app purchase flow. Replace old SKU? " + (oldSku != null));
                BillingFlowParams purchaseParams = BillingFlowParams.newBuilder()
                        .setSkuDetails(skuDetails)
                        .setOldSku(oldSku)
                        .build();
                mBillingClient.launchBillingFlow(mActivity, purchaseParams);
            }
        };

        executeServiceRequest(purchaseFlowRequest);
    }

    private void executeServiceRequest(Runnable runnable) {
        if (mIsServiceConnected) {
            runnable.run();
        } else {
            startServiceConnection(runnable);
        }
    }

    /**
     * Establish a connection to Google Play Billing Client
     * @param runnable Will be execute when connected to Google Play Billing
     */
    private void startServiceConnection(final Runnable runnable) {
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                int billingResponseCode = billingResult.getResponseCode();
                if (billingResponseCode == BillingClient.BillingResponseCode.OK) {
                    mIsServiceConnected = true;
                    if (runnable != null) {
                        runnable.run();
                    }
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                mIsServiceConnected = false;
            }
        });
    }

    @Override
    public void onPurchasesUpdated(final BillingResult billingResult, @Nullable final List<Purchase> purchases) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                int responseCode = billingResult.getResponseCode();
                if (responseCode == BillingClient.BillingResponseCode.OK) {
                    if (purchases != null) {
                        for (Purchase purchase : purchases) {
                            handlePurchase(purchase);
                        }
                    }
                } else if(responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                    Log.d(TAG, billingResult.getDebugMessage());
                    queryPurchases();
                } else if (responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                    Log.i(TAG, "onPurchasesUpdated() - user cancelled the purchase flow - skipping");
                } else {
                    Log.w(TAG, "onPurchasesUpdated() got unknown result code: " + responseCode);
                }
            }
        };
        executeServiceRequest(runnable);
    }

    private void handlePurchase(Purchase purchase) {
        if(!verifyValidSignature(purchase.getOriginalJson(), purchase.getSignature())) {
            Log.i(TAG, "Got a purchase: " + purchase + "; but signature is bad. Skipping...");
            return;
        }

        Log.d(TAG, "Got a verified purchase: " + purchase);

        if(purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            Log.d(TAG, "Purchase state: " +purchase.getPurchaseState());
            mBillingUpdatesListener.onPurchasesUpdated(purchase);

            if (!purchase.isAcknowledged()) {
                Log.e(TAG, "Purchase acknowledged");
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.getPurchaseToken())
                            .build();

                mBillingClient.acknowledgePurchase(acknowledgePurchaseParams, new AcknowledgePurchaseResponseListener() {
                    @Override
                    public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                        Log.d(TAG, "Acknowledge response return response with code: " + billingResult.getResponseCode());
                    }
                });
            }
        }
    }

    /**
     * Verifies that the purchase was signed correctly for this developer's public key.
     * <p>Note: It's strongly recommended to perform such check on your backend since hackers can
     * replace this method with "constant true" if they decompile/rebuild your app.
     * </p>
     */
    private boolean verifyValidSignature(String signedData, String signature) {
        try {
            return Security.verifyPurchase(BASE_64_ENCODED_PUBLIC_KEY, signedData, signature);
        } catch (IOException e) {
            Log.e(TAG, "Got an exception trying to validate a purchase: " + e);
            return false;
        }
    }

    /**
     * Clear the resources
     */
    public void destroy() {
        Log.d(TAG, "Destroying the manager.");

        if (mBillingClient != null && mBillingClient.isReady()) {
            mBillingClient.endConnection();
            mBillingClient = null;
        }
    }
}
