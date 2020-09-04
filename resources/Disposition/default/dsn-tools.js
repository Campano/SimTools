var sim = (function() { 
    'use strict';
    var SIM_PARAMS=null, APP=null, JS_BUNDLE = '/scripts/ajax/ajax-bundle.js', TOKEN_STORAGE_KEY = 'sim-token';
    var saveToken = t => window.localStorage.setItem(TOKEN_STORAGE_KEY, t);
    var readToken = () => window.localStorage.getItem(TOKEN_STORAGE_KEY)||false;
    var deleteToken = () => window.localStorage.removeItem(TOKEN_STORAGE_KEY);

    var init = (init_params,connect_params=false) => {
        dispOk('Initialise Simplicité helper');
        if(SIM_PARAMS !== null) return Promise.reject(dispKo('Already initialized.')); 
        if(!init_params.instanceRoot) return Promise.reject(dispKo('No Simplicité instanceRoot specified.'));
        SIM_PARAMS = {
            instanceRoot: init_params.instanceRoot,
            endpoint: init_params.endpoint||'api',
            params: init_params.params||null,
            log: init_params.log||'debug'
        };
        return loadJS(SIM_PARAMS.instanceRoot+JS_BUNDLE).then(verifySimpliciteLoaded).then(connect_params ? connect(connect_params) : Promise.resolve())
    }

    var loadJS = url => new Promise(resolve => {
        dispOk('Loading javascript : '+url);
        var script = document.createElement('script');
        script.onload = resolve;
        script.src = url;
        document.head.appendChild(script);
    });
    var verifySimpliciteLoaded = () => new Promise((resolve, reject)=> (typeof Simplicite=='undefined' ? reject(dispKo('Error loading library...')) : resolve()));

    var connect = connect_params => () => new Promise((resolve, reject)=>{
        if(SIM_PARAMS === null || Simplicite === undefined)
            reject(dispKo('Helper not initialized . Exit.'));
        else if(APP!==null)
            reject(dispKo('Already connected. Exit.'));
        else if(hasToken(connect_params)){
            dispOk('Token detected. Trying connection for token.');
            APP = new Simplicite.Ajax(SIM_PARAMS.instanceRoot,SIM_PARAMS.endpoint);
            APP.login(connected(resolve),tokenFailed(connect_params,resolve,reject),readToken(),SIM_PARAMS.params);
        }
        else if(hasUserCreds(connect_params)){
            dispOk('User credentials detected. Trying connection for user '+connect_params.login);
            APP = new Simplicite.Ajax(SIM_PARAMS.instanceRoot,SIM_PARAMS.endpoint,connect_params.login,connect_params.password);
            APP.login(connected(resolve),credentialsFailed(reject),false,SIM_PARAMS.params);
        }
        else
            reject(dispKo('No token nor user credentials provided. Exit.'));
    });

    var hasUserCreds = connect_params => connect_params.hasOwnProperty('login') && connect_params.hasOwnProperty('password');
    var hasToken = connect_params => connect_params.hasOwnProperty('tryToken') &&  connect_params.tryToken===true && readToken();
    
    var connected = resolve => loginResponse => {
        dispOk('Successful connection. Saving token.');
        saveToken(loginResponse.authtoken);
        APP.getGrant(g => {
            dispOk('Got grant data for user '+g.login+', responsibilities: '+JSON.stringify(APP.grant.responsibilities));
            resolve(APP);
        });
    };

    var tokenFailed = (connect_params,resolve,reject) => () => {
        cleanApp();
        if(hasUserCreds(connect_params)){
            dispKo('Token connection failed. Retrying connection with user credentials.');
            connect(connect_params)().catch(reject).then(resolve);
        }
        else
            reject(dispKo('Token connection failed. No user credentials. Exit.'));
    }

    var credentialsFailed = reject => () => {
        cleanApp();
        reject(dispKo('User credentials connection failed. Exit.'));
    };

    var logout = () => new Promise((resolve, reject) =>{
        dispOk('Logging out.');
        APP===null ? resolve() : APP.logout(()=>{ cleanApp(); resolve(); }, reject);
    });

    var cleanApp = ()=>{ APP=null; deleteToken(); };
    var dispOk = t=>disp('[sim helper] ✅ '+t);
    var dispKo = t =>disp('[sim helper] ❌ '+t);
    var disp = (content, output=SIM_PARAMS&&SIM_PARAMS.log)  => {
        if(output === 'debug')
            console.debug(content);
        else if(typeof(output)==='string' && document && document.getElementById(output)!==null){
            var p = document.createElement("p"); p.textContent = content;
            document.getElementById(output).appendChild(p).appendChild(document.createElement("hr"));
        }
        else if(output!==false)
            console.log(content);
        return content;
    }

    return { init:init, connect:connect, logout:logout, getApp:()=>APP, disp:disp };
})();