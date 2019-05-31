package com.indevelopment.sock.data;

import com.indevelopment.sock.model.Rule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RuleData {
    public static final List<Rule> rules = Collections.synchronizedList(new ArrayList<Rule>());
}
