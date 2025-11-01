package com.miso.vinilos.rules

import android.graphics.Bitmap
import androidx.test.runner.screenshot.Screenshot
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.IOException

class ScreenshotTestRule : TestWatcher() {
    override fun finished(description: Description) {
        super.finished(description)
        takeScreenshot(description)
    }

    private fun takeScreenshot(description: Description) {
        val filename = description.testClass.simpleName + "-" + description.methodName
        try {
            Screenshot.capture()
                .setName(filename)
                .setformat(Bitmap.CompressFormat.PNG)
                .process()
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }
}