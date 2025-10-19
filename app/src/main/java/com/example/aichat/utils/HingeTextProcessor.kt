package com.example.aichat.utils


class HingeTextProcessor : TextProcessor {
    override fun processText(rawText: String): String {
        val lines = rawText.split("\n")
        val messages = mutableListOf<String>()

        for (line in lines) {
            val trimmedLine = line.trim()

            // Skip empty lines and separators
            if (trimmedLine.isEmpty() || trimmedLine == "---") continue

            // Check if line contains a message (starts with "You:" or ends with ":")
            when {
                trimmedLine.startsWith("You:") -> {
                    messages.add(trimmedLine)
                }
                trimmedLine.matches(".*:\\s*.+".toRegex()) &&
                        !trimmedLine.contains("AM") && !trimmedLine.contains("PM") -> {
                    // Person's message (name followed by colon and message)
                    messages.add(trimmedLine)
                }
                // Keep timestamps that contain day/time info
                trimmedLine.matches(".*,\\s*\\w+\\s+\\d+\\s+\\d+:\\d+\\w+".toRegex()) -> {
                    messages.add("---")
                    messages.add(trimmedLine)
                    messages.add("---")
                }
            }
        }

        return messages.joinToString("\n")
    }

    override fun canHandle(packageName: String): Boolean {
        return packageName == "co.hinge.app"
    }
}