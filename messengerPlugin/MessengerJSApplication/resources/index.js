"use strict"

let login = require("facebook-chat-api")
let fs = require("fs")
let os = require("os")
let path = require("path")
let process = require("process")

let api = null
let credentials = JSON.parse(fs.readFileSync(path.resolve(os.homedir(), ".convergence", "facebookCredentials.json")).toString())
const functionNames = new Set([
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

function startServer() {
    function send(...args) {
        console.log(args.join("\t"))
    }

    process.stdin.resume()
    process.stdin.setEncoding("utf8")
    process.stdin.on("data", function onData(data) {
        let body = null;
        try {
            body = JSON.parse(data)
        } catch (e) {
            send(JSON.stringify({err: e.toString(), data: data}))
            return
        }
        let id = body["id"]
        // Check if the JSON is structured correctly
        if (!("method" in body)) {
            send(JSON.stringify({err: "Method not specified!", id: id}))
            return
        }
        if (!functionNames.has(body["method"])) {
            send(JSON.stringify({err: "Method not valid!", id: id}))
            return
        }
        if (!("arguments" in body)) {
            send(JSON.stringify({err: "Arguments not provided", id: id}))
            return
        }
        // Run the API method
        if (body["method"] === "listen") {
            console.error("Listening...")
            console.error("api=" + api)
            api.listen(function (err, message) {
                let response = JSON.stringify(err ? {id: id, success: false, err: err.message} : message)
                console.error(response)
                send(response)
            })
            send(JSON.stringify({id: id, success: true}))
        } else {
            api[body["method"]](...body["arguments"], function (err, obj) {
                if (err) {
                    send(body, 500, err.message)
                    return
                }
                obj = obj || {}
                obj.id = id
                let response = {}
                Object.keys(obj).forEach(function (k) {
                    response[k] = obj[k]
                })
                send(JSON.stringify(response))
                console.error(JSON.stringify(response))
            })
        }
    })
    api.listen()
}

if (credentials["email"]) {
    //*
    login({email: credentials["email"], password: credentials["password"]}, {
        listenEvents: true,
        autoMarkRead: true,
        autoMarkDelivery: true,
        userAgent: "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:77.0) Gecko/20100101 Firefox/77.0",
        forceLogin: true
    }, function (err, _api) {
        if (err)
            throw err
        api = _api
        startServer()
    })
    //*/
    //startServer()
    console.error("Started!")
} else
    console.error("Put in an email for your account!")

