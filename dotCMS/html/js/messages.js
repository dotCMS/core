var messagesCount = 0;
var messageYIncrement = 60;
var occupiedPositions = new Array();

function showDotCMSSystemMessage(message) {
    showDotCMSSystemMessage(message, false);
}

function showDotCMSSystemMessage(message, isError) {

    var position = 40;

    if (occupiedPositions.length > 0)
        position = occupiedPositions[occupiedPositions.length - 1] + messageYIncrement;
    occupiedPositions.push(position);


    var className = isError ? 'systemErrorsHolder' : 'systemMessagesHolder';
    var holdingDiv = dojo.create("div", {
        id: "systemMessagesWrapper" + messagesCount,
        className: className,
        style: {top: position + '%'}
    }, dojo.body());

    var className = isError ? 'errorMessages' : 'systemMessages';
    var systemMessages = dojo.create("div", {
        id: "systemMessages" + messagesCount,
        className: className
    }, holdingDiv);

    systemMessages.innerHTML = message;

    dojo.connect(dijit.byId("systemMessages"), "onClick", hideDotCMSSystemMessage);

    var hideFn = dojo.partial(hideDotCMSSystemMessage, messagesCount);
    dojo.connect(holdingDiv, 'onclick', hideFn);

    var hideFn = dojo.partial(hideDotCMSSystemMessage, messagesCount);
    var fadeOutFn = dojo.fadeOut({node: "systemMessages" + messagesCount, delay: 10, duration: 0, onEnd: hideFn}).play;

    var fadeIn = dojo.fadeIn({node: "systemMessages" + messagesCount, duration: 5000, onEnd: fadeOutFn});
    fadeIn.play();

    var ttl = message.split(" ").length;
    ttl = ttl * 200;
    if (ttl < 1000) {
        ttl = 1000;
    }


    hideMessagesHandler = setTimeout(hideFn, ttl);

    messagesCount++;

}

function hideDotCMSSystemMessage(messageId) {

    var id = dojo.byId("systemMessagesWrapper" + messageId);
    if (id == null) {
        return;
    }

    var currentY = parseInt(id.style.top);
    occupiedPositions = dojo.filter(occupiedPositions, function (x) {
        return x != currentY;
    });


    dojo.fadeOut({node: "systemMessagesWrapper" + messageId}).play();
    dojo.destroy("systemMessagesWrapper" + messageId);


}

var hideErrorsHandler;

function showDotCMSErrorMessage(message) {
    showDotCMSSystemMessage(message, true);
}
