var HelloPlugin = 
{     
	callNativeFunction: function (success, fail, resultType) 
	{       
		return cordova.exec( success, fail, "com.yesorno.HelloPlugin", "nativeAction", [resultType]);     
	} 
};