var exec = require('cordova/exec');

var BTPrinter = {
    status: function (fnSuccess, fnError) {
        exec(fnSuccess, fnError, "BluetoothPrinter", "status", []);
    },
    list: function (fnSuccess, fnError) {
        exec(fnSuccess, fnError, "BluetoothPrinter", "list", []);
    },
    connect: function (fnSuccess, fnError, name) {
        exec(fnSuccess, fnError, "BluetoothPrinter", "connect", [name]);
    },
    disconnect: function (fnSuccess, fnError) {
        exec(fnSuccess, fnError, "BluetoothPrinter", "disconnect", []);
    },
    print: function(fnSuccess, fnError, str){
        exec(fnSuccess, fnError, "BluetoothPrinter", "print", [str]);
    },
    printPOSCommand: function(fnSuccess, fnError, str){
        exec(fnSuccess, fnError, "BluetoothPrinter", "printPOSCommand", [str]);
    }
};

module.exports = BTPrinter;