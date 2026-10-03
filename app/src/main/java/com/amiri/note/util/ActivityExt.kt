package com.amiri.note.util

import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity

/** Walks the context wrapper chain to find the hosting FragmentActivity. */
fun Context.findActivity(): FragmentActivity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
