<!--
/*******************************************************************************
Copyright 2009-2012 Ellucian Company L.P. and its affiliates.
*******************************************************************************/
-->
<%@ page contentType="text/html;charset=UTF-8" defaultCodec="none" %>

<html>
    <head>
        <title><g:message code="net.hedtech.banner.productTitle"/></title>
        <link rel="stylesheet" href="${resource(plugin: 'bannerCore', dir: 'css', file: 'timeout.css')}"/>
        <link rel="stylesheet" href="${resource(plugin: 'bannerCore', dir: 'css', file: 'main.css')}"/>
        <link rel="stylesheet" href="${resource(plugin: 'bannerCore', dir: 'css', file: 'button.css')}"/>
        <g:set var="actionLabel" value="${g.message(code: 'net.hedtech.banner.access.denied.dialog.action')}"/>
        <link rel="shortcut icon" href="${resource(plugin: 'bannerCore', dir: 'images', file: 'favicon.ico')}" type="image/x-icon"/>
    </head>
    <body>
        <div class="error">
            <div class="errorBox">
              <div class="errorMessage"><b><g:message code="net.hedtech.banner.errors.serverError.message"/> </b><br>
                <g:message code="net.hedtech.banner.errors.serverError.error"/>
                ${exception?.message?.encodeAsHTML()}   <br /> <br />
              </div>
            </div>
           <div class="errorBackButton">
           <g:link uri="${returnHomeLinkAddress}">
                <input type="button"  value="${g.message(code:'net.hedtech.banner.errors.serverError.backToHomeButton.label')}" class="secondary-button" />
           </g:link>
           </div>
        </div>
    </body>
</html>
