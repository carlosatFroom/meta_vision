package com.metavision.glassreader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmsNotificationListenerTest {

    @Test
    fun normalSms_returnsSenderAndText() {
        val parsed = SmsNotificationParser.parse(title = "+15551234567", text = "Hello there", bigText = null)
        assertEquals(ParsedNotification(sender = "+15551234567", body = "Hello there"), parsed)
    }

    @Test
    fun bigText_takesPriorityOverText() {
        val parsed = SmsNotificationParser.parse(
            title = "Mom",
            text = "Can you pick up…",
            bigText = "Can you pick up milk on the way home?",
        )
        assertEquals("Can you pick up milk on the way home?", parsed?.body)
    }

    @Test
    fun blankBigText_fallsBackToText() {
        val parsed = SmsNotificationParser.parse(title = "Mom", text = "Hi", bigText = "   ")
        assertEquals("Hi", parsed?.body)
    }

    @Test
    fun missingOrBlankBody_returnsNull() {
        assertNull(SmsNotificationParser.parse(title = "Mom", text = null, bigText = null))
        assertNull(SmsNotificationParser.parse(title = "Mom", text = "", bigText = " "))
    }

    @Test
    fun missingOrBlankTitle_usesUnknownSender() {
        assertEquals(
            SmsNotificationParser.UNKNOWN_SENDER,
            SmsNotificationParser.parse(title = null, text = "Hi", bigText = null)?.sender,
        )
        assertEquals(
            SmsNotificationParser.UNKNOWN_SENDER,
            SmsNotificationParser.parse(title = "  ", text = "Hi", bigText = null)?.sender,
        )
    }
}
