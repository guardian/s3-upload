
exports.handler = function (event, context) {
    console.log(event);
    console.log(context);
    context.succeed(true);
};