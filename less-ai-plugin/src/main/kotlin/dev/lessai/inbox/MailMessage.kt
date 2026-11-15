package dev.lessai.inbox

/** One message parsed from an agent's inbox file: sender, ISO time string and body text. */
data class MailMessage(val from: String, val time: String, val body: String)
