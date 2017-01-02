/*******************************************************************************
 Copyright 2016  Ellucian Company L.P. and its affiliates.
 *******************************************************************************/

class CasBootStrap {
    def authenticationProcessingFilter
    def casAuthenticationFailureHandler
    def init = {servletContext ->
        authenticationProcessingFilter.authenticationFailureHandler = casAuthenticationFailureHandler
    } }