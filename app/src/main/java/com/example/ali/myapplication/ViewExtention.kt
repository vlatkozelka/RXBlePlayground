package com.example.ali.myapplication

import android.view.View

var View.isGone: Boolean
    get() {
        return true
    }
    set(newValue) {
        this.visibility = if (newValue) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }