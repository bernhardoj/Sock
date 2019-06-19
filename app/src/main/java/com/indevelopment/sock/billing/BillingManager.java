package com.indevelopment.sock.billing;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles all the interactions with Play Store (via Billing library), maintains connection to
 * it through BillingClient and caches temporary states/data if needed
 */
public class BillingManager implements PurchasesUpdatedListener {
    private static final String TAG = "BillingManager";

    private boolean show = true;

    /**
     * A reference to BillingClient
     **/
    private BillingClient mBillingClient;

    /**
     * A reference to FirebaseFunctions
     */
    private FirebaseFunctions mFunctions;

    private final Activity mActivity;

    private List<SkuDetails> mSkuDetails = new ArrayList<>();

    public static final String ITEM_SKU = "indevelopment.sock.premium";

    private String[] skuList = new String[]{ITEM_SKU};

    public interface BillingUpdatesListener {
        void onPurchasesUpdated(Purchase purchase, int responseCode);
    }

    /**
     * True if billing service is connected now.
     */
    private boolean mIsServiceConnected;

    private boolean mIsNetworkAvailable;

    private final BillingUpdatesListener mBillingUpdatesListener;

    public BillingManager(final Activity activity, final BillingUpdatesListener billingUpdatesListener) {
        Log.d(TAG, "Creating Billing client.");
        mActivity = activity;
        mBillingUpdatesListener = billingUpdatesListener;
        mBillingClient = BillingClient.newBuilder(mActivity).setListener(this).enablePendingPurchases().build();

        mFunctions = FirebaseFunctions.getInstance();

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
                querySkuDetails(skuList);
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
                            mIsNetworkAvailable = true;
                            Log.d(TAG, "Querying SKU details elapsed time: " + (System.currentTimeMillis() - time)
                                    + "ms with List size: " + mSkuDetails.size());
                        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE) {
                            mIsNetworkAvailable = false;
                            Log.w(TAG, "Failed to querying SKU Details");
                        }
                    }
                });
            }
        };

        executeServiceRequest(runnable);
    }

    public void queryPurchases() {
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
        if (mIsNetworkAvailable) {
            try {
                initiatePurchaseFlow(mSkuDetails.get(index), null);
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        } else {
            displayToast("No internet connection");
            Log.e(TAG, "No internet connection!");
            querySkuDetails(skuList);
        }
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
     *
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
                if (responseCode == BillingClient.BillingResponseCode.OK || responseCode == BillingClient.BillingResponseCode.ITEM_NOT_OWNED) {
                    if (purchases != null) {
                        for (Purchase purchase : purchases) {
                            handlePurchase(purchase, responseCode);
                        }
                    }
                } else if (responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                    Log.d(TAG, billingResult.getDebugMessage());
                    displayToast("Item already owned");
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

    private void handlePurchase(final Purchase purchase, final int responseCode) {
        if (responseCode == BillingClient.BillingResponseCode.OK) {
            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                if (!purchase.isAcknowledged()) {
                    verifyValidSignature(purchase.getOriginalJson(), purchase.getSignature())
                            .addOnCompleteListener(new OnCompleteListener<Map<String, Object>>() {
                                @Override
                                public void onComplete(@NonNull Task<Map<String, Object>> task) {
                                    if (task.isSuccessful()) {
                                        Log.d(TAG, "Task successfully done!");

                                        Map<String, Object> result = task.getResult();

                                        if (result != null) {
                                            if ((boolean) result.get("verified")) {
                                                Log.d(TAG, "Got a verified purchase: ");
                                                Log.d(TAG, "Purchase state: " + purchase.getPurchaseState());

                                                AcknowledgePurchaseParams acknowledgePurchaseParams =
                                                        AcknowledgePurchaseParams.newBuilder()
                                                                .setPurchaseToken(purchase.getPurchaseToken())
                                                                .build();

                                                mBillingClient.acknowledgePurchase(acknowledgePurchaseParams, new AcknowledgePurchaseResponseListener() {
                                                    @Override
                                                    public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                                                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                                            mBillingUpdatesListener.onPurchasesUpdated(purchase, BillingClient.BillingResponseCode.OK);
                                                            Log.d(TAG, "Purchase acknowledged");
                                                        } else {
                                                            Log.d(TAG, "Acknowledge response return response with code: " + billingResult.getResponseCode());
                                                        }
                                                    }
                                                });
                                            } else {
                                                Log.i(TAG, "Purchase is not valid");
                                            }
                                        } else {
                                            Log.w(TAG, "No result found from the task!");
                                        }
                                    } else {
                                        Log.e(TAG, "Firebase Cloud Functions encounter an error");
                                    }
                                }
                            });
                } else {
                    mBillingUpdatesListener.onPurchasesUpdated(purchase, BillingClient.BillingResponseCode.OK);
                }
            }
        } else {
            mBillingUpdatesListener.onPurchasesUpdated(purchase, BillingClient.BillingResponseCode.ITEM_NOT_OWNED);
        }
    }

    /**
     * Verifies that the purchase was signed correctly for this developer's public key.
     */
    @SuppressWarnings({"unchecked"})
    private Task<Map<String, Object>> verifyValidSignature(String signedData, String signature) {
        // Create the arguments to the callable function.
        Map<String, Object> data = new HashMap<>();
        data.put("signedData", signedData);
        data.put("signature", signature);

        return mFunctions.getHttpsCallable("verifySignature")
                .call(data)
                .continueWith(new Continuation<HttpsCallableResult, Map<String, Object>>() {
                    @Override
                    public Map<String, Object> then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        HttpsCallableResult result = task.getResult();
                        if (result != null) {
                            return (Map<String, Object>) result.getData();
                        }

                        return null;
                    }
                });
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

    private void displayToast(String message) {
        if (show) {
            Toast.makeText(mActivity, message, Toast.LENGTH_SHORT).show();
            show = false;
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                show = true;
            }
        }, 2000);
    }
}
