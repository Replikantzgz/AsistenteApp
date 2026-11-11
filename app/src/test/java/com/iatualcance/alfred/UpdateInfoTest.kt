package com.iatualcance.alfred

import com.iatualcance.alfred.update.UpdateInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateInfoTest {
    private val json = """{"versionCode":16,"versionName":"1.3.6","url":"https://github.com/x/y/releases/download/v16/Alfred.apk","sha256":"ab12","notes":"Tema Pokémon","date":"2026-11-12"}"""

    @Test fun parsesPublishedFile() {
        val i = UpdateInfo.parse(json)!!
        assertEquals(16, i.versionCode); assertEquals("1.3.6", i.versionName); assertEquals("ab12", i.sha256); assertEquals("Tema Pokémon", i.notes)
    }

    @Test fun onlyNewerCodesCount() {
        val i = UpdateInfo.parse(json)!!
        assertTrue(i.isNewerThan(15)); assertFalse(i.isNewerThan(16)); assertFalse(i.isNewerThan(99))
    }

    @Test fun rejectsBadOrInsecureInput() {
        assertNull(UpdateInfo.parse("")); assertNull(UpdateInfo.parse("<html>404</html>"))
        assertNull(UpdateInfo.parse("""{"versionCode":16,"url":"http://insecure/Alfred.apk"}"""))
        assertNull(UpdateInfo.parse("""{"versionCode":0,"url":"https://x/y.apk"}"""))
        assertNotNull(UpdateInfo.parse("""{"versionCode":2,"url":"https://x/y.apk"}"""))
    }
}
