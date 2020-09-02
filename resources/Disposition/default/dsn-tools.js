var sim = (function(globals) {
    'use strict';
    var JS_BUNDLE = '/scripts/ajax/ajax-bundle.js';
    var GLOBALS = null;
    var TOKEN_STORAGE_KEY = 'sim-token';
    var APP = null;

    function init(g, user){
        if(GLOBALS !== null){
            return Promise.reject('Already initialized.');
        }
        GLOBALS = g;
        disp('init');
        return loadJS(GLOBALS.instanceRoot+JS_BUNDLE)
        .then(user!==undefined ? connect(user) : Promise.resolve())
    }

    // Connect using tokens if available
    function connect(params){
        return () => new Promise(function(resolve, reject){
            var userCreds = params.hasOwnProperty('login') && params.hasOwnProperty('password') ? {login: params.login, password:params.password} : false;
            var token = params.hasOwnProperty('tryToken') &&  params.tryToken===true ? window.localStorage.getItem(TOKEN_STORAGE_KEY)||false : false;
            if(GLOBALS === null || Simplicite === undefined){
                disp('SIM not initialized');
                reject(); 
            }
            else if(APP!==null){
                disp('Already connected'); 
                resolve(APP);
            }
            else if(token===false && userCreds===false){
                disp('Impossible to connect without connection params'); 
                reject();
            }
            else if(token!==false){
                disp('Start connection for token '+TOKEN_STORAGE_KEY);
                APP = new Simplicite.Ajax(
                    GLOBALS.instanceRoot,
                    GLOBALS.endpoint
                );
                APP.login(
                    connectSuccess(resolve),
                    () => {
                        disp('Failed login for token. Removing token.');
                        window.localStorage.removeItem(TOKEN_STORAGE_KEY);
                        APP = null;
                        //retry if case failed for expired token
                        if(userCreds){
                            disp('UserCreds present, trying userCred Connection.');
                            connect(params)().catch(reject).then(resolve);
                        }
                        else
                            reject();
                    },
                    token,
                    GLOBALS.params
                );
            }
            else{
                disp('Start connection for user '+userCreds.login);
                APP = new Simplicite.Ajax(
                    GLOBALS.instanceRoot,
                    GLOBALS.endpoint,
                    userCreds.login,
                    userCreds.password
                );
                APP.login(
                    connectSuccess(resolve),
                    () => {
                        disp('Failed login');
                        window.localStorage.removeItem(TOKEN_STORAGE_KEY);
                        APP = null;
                        reject();
                    },
                    false,
                    GLOBALS.params
                );
            }
        });
    }

    function connectSuccess(callback){
        return (s) => {
            disp('Successful connection, saving token...');
            window.localStorage.setItem(TOKEN_STORAGE_KEY, s.authtoken);
            APP.getGrant(g => {
                disp('Got grant data for user '+g.login+', responsibilities: '+JSON.stringify(APP.grant.responsibilities));
                callback(APP);
            });
        };
    }

    function logout(){
        return new Promise(function(resolve, reject){
            disp("Logging out...")
            if(APP!==null){
                APP.logout(
                    ()=>{
                        window.localStorage.removeItem(TOKEN_STORAGE_KEY);
                        APP=null;
                        resolve();
                    }, 
                    reject
                );
            }
            else
                resolve();
        });
    }

    function getApp(){
	return APP;
    }

    function loadJS(url){
        return new Promise(function(resolve, reject){
            var script = document.createElement('script');
            script.onload = function(){
                resolve();
            };
            script.src = url;
            document.head.appendChild(script);
        });
    }

    function disp(content){
        Utils.disp('[sim] '+content, GLOBALS.log);
    }

    return {
        init : init,
        connect : connect,
        logout: logout,
        getApp: getApp
    };
})();

var Utils = (function(){
	function forceDownload(name, mime, b64content){
		if (navigator.msSaveBlob) { // IE10+ : (has Blob, but not a[download] or URL)
			var blob = b64toBlob(decodeURIComponent(b64content), mime);
			return navigator.msSaveBlob(blob, name);
		}
		else {
			var link = document.createElement("a");
			document.body.appendChild(link);
	        link.setAttribute("type", "hidden");
			link.href = "data:"+mime+";base64,"+b64content;
			link.download = name;
			link.target = "blank";
			link.click();
		}
    }
    
    function disp(content, output){ //false, true, 'debug', 'container-id'
        if(output === false){
            return;
        }
        if(output === true || output === undefined){
            console.log(content);
            return;
        }
        else if(output === 'debug'){
            console.debug(content);
        }
        else if(document.getElementById(output) !== null){
            var p = document.createElement("p");
            p.textContent = content;
            document.getElementById(output)
                .appendChild(p)
                .appendChild(document.createElement("hr"));
        }
    }

	function base64dataFromBoDoc(boDoc){
		return 'data:'+boDoc.mime+';base64,'+boDoc.content;
	}

	function b64toBlob(b64Data, contentType) {
		// Used for download data on IE10+ (need to convert base64 to blob)
		contentType = contentType || '';
		var sliceSize = 512;
		//b64Data = b64Data.replace(/^[^,]+,/, '');
		//b64Data = b64Data.replace(/\s/g, '');
		var byteCharacters = window.atob(b64Data);
		var byteArrays = [];

		for (var offset = 0; offset < byteCharacters.length; offset += sliceSize) {
		    var slice = byteCharacters.slice(offset, offset + sliceSize);

		    var byteNumbers = new Array(slice.length);
		    for (var i = 0; i < slice.length; i++) {
		        byteNumbers[i] = slice.charCodeAt(i);
		    }

		    var byteArray = new Uint8Array(byteNumbers);

		    byteArrays.push(byteArray);
		}

		var blob = new Blob(byteArrays, {type: contentType});
		return blob;
	}

	return {
		forceDownload: forceDownload,
		base64dataFromBoDoc: base64dataFromBoDoc,
        b64toBlob: b64toBlob,
        disp: disp
	};
})();
