import login from "facebook-chat-api";

function init(outParam) {
    login({email: email, password: password}, function (error, api) {
        if (error)
            return console.error(error);
        outParam.setApi(api);
    });
}


