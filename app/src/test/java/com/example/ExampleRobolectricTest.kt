package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import cu.todus.download.data.DownloadItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("toDus Download", appName)
  }

  @Test
  fun `verify download item speed and percentage formatting`() {
    val item = DownloadItem(
        id = "test_1",
        url = "https://s3.todus.cu/test.mp4",
        name = "test.mp4",
        totalBytes = 100 * 1024 * 1024L,
        downloadedBytes = 50 * 1024 * 1024L,
        speedBps = 2 * 1024 * 1024L,
        status = DownloadItem.Status.DOWNLOADING
    )
    assertEquals(50, item.percentage)
    assertTrue(item.humanSpeed().contains("MB/s"))
    assertTrue(item.isVideo)
  }
}
