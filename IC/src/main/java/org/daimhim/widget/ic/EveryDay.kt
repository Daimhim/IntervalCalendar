package org.daimhim.widget.ic

class EveryDay {
    /**
     * Item 样式
     */
    @JvmField
    var viewType = 0
    /**
     * 是否可用
     */
    @JvmField
    var enable = true
    @JvmField
    var millis: Long = -1
    var year = 0
    @JvmField
    var moon = 0
    @JvmField
    var day = 0
    @JvmField
    var hour = 0
    @JvmField
    var minute = 0
    @JvmField
    var second = 0
}