"use strict"

let login = require("facebook-chat-api")
let http = require("http")
let fs = require("fs")
let os = require("os")
let path = require("path")
let ws = require("ws")

let api = null
let credentials = JSON.parse(fs.readFileSync(path.resolve(os.homedir(), ".convergence", "facebookCredentials.json")).toString())
if (credentials["email"]) {
    let appState = null
    if (fs.existsSync(path.resolve(os.homedir(), ".convergence", "messenger", " appstate.json")))
        appState = JSON.parse(fs.readFileSync(path.resolve(os.homedir(), ".convergence", "messenger", " appState.json")).toString("utf8"))

    login(appState == null ? {email: credentials["email"], password: credentials["password"]} : {appState: appState}, {
        forceLogin: true,
        listenEvents: true,
        autoMarkRead: true,
        userAgent: "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:74.0) Gecko/20100101 Firefox/74.0"
    }, function (err, _api) {
        if (err)
            throw err
        api = _api
        startServer()
        fs.writeFile(path.resolve(os.homedir(), ".convergence", "messenger", "appstate.json"), JSON.stringify(api.getAppState()), function (err) {
            if (err)
                console.error(err)
        })
    })
    credentials = null

    let functionNames = new Set([
        "addUserToGroup",
        "changeAdminStatus",
        "changeArchivedStatus",
        "changeBlockedStatus",
        "changeGroupImage",
        "changeNickname",
        "changeThreadColor",
        "changeThreadEmoji",
        "createPoll",
        "deleteMessage",
        "deleteThread",
        "forwardAttachment",
        "getAppState",
        "getFriendsList",
        "getThreadInfo",
        "getThreadList",
        "getThreadPictures",
        "getUserId",
        "getUserInfo",
        "handleMessageRequest",
        "listen",
        "markAsReadAll",
        "muteThread",
        "removeUserFromGroup",
        "resolvePhotoUrl",
        "sendMessage",
        "sendTypingIndicator",
        "setMessageReaction",
        "setOptions",
        "unsendMessage"
    ])

    function send(response, code, reason, contentType = "text/plain") {
        response.writeHead(code, reason, {"Content-Type": contentType})
        response.write(reason)
        response.end()
    }

    // This is intentionally empty, it'll be overwritten later.
    let sendWs = function (message) {
    }

    function startServer() {
        let server = http.createServer(function (request, response) {
            if (request.connection.remoteAddress !== '127.0.0.1')
                return
            let body = null;
            try {
                body = JSON.parse(request.read())
            } catch (e) {
                send(response, 400, e.message)
                return
            }
            // Check if the JSON is structured correctly
            if (!("method" in body)) {
                send(response, 400, "Method not specified")
                return
            }
            if (!(body["method"] in functionNames)) {
                send(response, 400, "Method not valid")
                return
            }
            if (!("arguments" in body)) {
                send(response, 400, "Arguments not provided")
                return
            }
            // Run the API method
            if (body["method"] === "listen") {
                api.listen(function (err, message) {
                    if (err) {
                        sendWs(err.message)
                        return
                    }
                    sendWs(JSON.stringify(message))
                    console.log(JSON.stringify(message))
                })
            } else {
                api[body["method"]](...body["arguments"], function (err, ...args) {
                    if (err) {
                        send(response, 500, err.message)
                        return
                    }
                    response.writeHead(200, "OK", {"Content-Type": "application/json"})
                    response.write(JSON.stringify(args))
                    response.end()
                })
            }
        })
        server.listen(5672)
        let wsServer = new ws.Server({port: 50672})
        wsServer.on("connection", function (ws) {
            sendWs = function (message) {
                ws.send(message)
            }
        })
    }
} else
    console.error("Put in an email for your account!")

