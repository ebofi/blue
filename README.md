# Cordova-Bluetooth-Printer-Plugin
A cordova plugin for bluetooth printer for android platform.

This code is being adapted from a fork of Cordova-Plugin-Bluetooth-Printer, of free use and modifications that will arise for the improvement of the plugin.

## Support
- Text
- POS
- ZPL and ZPL Commands

## Install
Using the Cordova CLI and NPM, run:

```
cordova plugin add https://github.com/TruewindIT/Cordova-Bluetooth-Printer-Plugin
```

## Usage
Get list of paired bluetooth devices, including printers, if any:

### List of bluetooth devices available

``` javascript
BTPrinter.list(function(data){
        console.log("Success");
        console.log(data); \\list of printer in data array
    },function(err){
        console.log("Error");
        console.log(err);
    })
```

### Check Bluetooth status

```javascript
BTPrinter.status(function(data){
	console.log("Success");
	console.log(data) // bt status: true or false
},function(err){
	console.log("Error");
	console.log(err)
});
```

### Connect printer

``` javascript
BTPrinter.connect(function(data){
	console.log("Success");
	console.log(data)
},function(err){
	console.log("Error");
	console.log(err)
}, "PrinterName")
```

### Disconnect printer

```javascript
BTPrinter.disconnect(function(data){
	console.log("Success");
	console.log(data)
},function(err){
	console.log("Error");
	console.log(err)
}, "PrinterName");
```

### Print simple string

```javascript
BTPrinter.printText(function(data){
    console.log("Success");
    console.log(data)
},function(err){
    console.log("Error");
    console.log(err)
}, "String to Print")
```

### Print a POS command

``` javascript
BTPrinter.printPOSCommand(function(data){
    console.log("Success");
    console.log(data)
},function(err){
    console.log("Error");
    console.log(err)
}, "0C")

//OC is a POS command for page feed
```