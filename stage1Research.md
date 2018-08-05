# Dynamically Loading from a jar at runtime
https://dzone.com/articles/add-jar-file-java-load-path

The name of the class loaded by the jar needs to be standardized, or
the jar needs to include some kind of file specifying which class to load.
Files inside of a jar can be read directly at runtime as if they were in
a normal folder, so reading which class to load from a file at runtime
is possible.


# Interface specs
- Base
    - On Receive message (Protocol, chat, message, sender)
    - Send message (Protocol, chat, message, sender)
    - Get bot's name (Protocol, chat)
    - List users (Protocol, chat)
- Nicknames
    - Get user's nickname (Protocol, chat, user)
    - Get bot's nickname (Protocol, chat)
- Images
    - Send image (Protocol, chat, base64data/URL)
    - Receive image (Protocol, chat, base64data/URL)
- Editing other user's messages
    - Edit Received Message (Protocol, chat, message, sender, newMessage)
    - On message edited (Protocol chat, oldMessage,  editor, newMessage)
        - May have the option to edit the message immediately, since this interface doesn't including indexing past messages?
- Message History (Not all protocols support this, or the bot API might be rate limited)
    - Get user's messages (Protocol, chat, user, sinceDateTime)
    - Get all messages (Protocol, chat, user, sinceDateTime)
- Mentioning users
    - Get Mention Text (Protocol, chat, user)
    - On Bot mentioned (Protocol, chat, message, sender)
- Typing Status
    - On user start typing (Protocol, chat, user)
    - On user stop typing (Procotol, chat, user)
    - Set bot typing status (status)
- Stickers? (Same as images?)
    - Send sticker (Protocol, chat, stickerID?)
    - On Receive sticker (Protocol, chat, sender)
- User status (Skype, IRC)
    - Set bot's status (Protocol, chat, status)
    - Get user's status (Protocol, chat, user)
- User availability (Online, offline, away, do not disturb)
    - Set bot's availability (Protocol, chat, availability)
    - Get user's availability (Protocol, chat, user)
    - List users with given availability (Protocol, chat, availability)
- Read status
    - Get read status of a message (Requires message history) (Protocol, chat, messageId)
    - Set read status of a message (Requires message history) (Protocol, chat, messageId, status)
