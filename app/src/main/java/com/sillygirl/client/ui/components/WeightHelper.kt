package com.sillygirl.client.ui.components

import androidx.compose.ui.Modifier

/**
 * Weight helper that avoids Kotlin 2.1.20 + Compose BOM
 * internal val weight conflict in RowColumnParentData.
 *
 * Uses fully qualified name to call the extension function
 * without triggering the internal val shadowing bug.
 */
fun Modifier.weight_(proportion: Float): Modifier {
    // Call the extension function using fully qualified name
    // This avoids importing the name 'weight' which would shadow the internal val
    return androidx.compose.foundation.layout.weight(this, proportion)
}
