package com.focusfloat.app.digitalwellbeing

import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo

internal interface DigitalWellbeingAutomationNode {
    val packageName: String?
    val text: String?
    val contentDescription: String?
    val stateDescription: String?
    val isCheckable: Boolean
    val isChecked: Boolean
    val isSelected: Boolean
    val isEnabled: Boolean
    val isClickable: Boolean
    val isScrollable: Boolean
    val parent: DigitalWellbeingAutomationNode?
    val children: List<DigitalWellbeingAutomationNode>
    val boundsArea: Int

    fun click(): Boolean
    fun scrollForward(): Boolean
}

internal class AccessibilityNodeAutomationNode(
    private val node: AccessibilityNodeInfo,
) : DigitalWellbeingAutomationNode {
    override val packageName: String?
        get() = node.packageName?.toString()
    override val text: String?
        get() = node.text?.toString()
    override val contentDescription: String?
        get() = node.contentDescription?.toString()
    override val stateDescription: String?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            node.stateDescription?.toString()
        } else {
            null
        }
    override val isCheckable: Boolean
        get() = node.isCheckable
    override val isChecked: Boolean
        get() = node.isChecked
    override val isSelected: Boolean
        get() = node.isSelected
    override val isEnabled: Boolean
        get() = node.isEnabled
    override val isClickable: Boolean
        get() = node.isClickable
    override val isScrollable: Boolean
        get() = node.isScrollable
    override val parent: DigitalWellbeingAutomationNode?
        get() = node.parent?.let(::AccessibilityNodeAutomationNode)
    override val children: List<DigitalWellbeingAutomationNode>
        get() = buildList {
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let { add(AccessibilityNodeAutomationNode(it)) }
            }
        }
    override val boundsArea: Int
        get() {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            return bounds.width().coerceAtLeast(0) * bounds.height().coerceAtLeast(0)
        }

    override fun click(): Boolean = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)

    override fun scrollForward(): Boolean = node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
}
