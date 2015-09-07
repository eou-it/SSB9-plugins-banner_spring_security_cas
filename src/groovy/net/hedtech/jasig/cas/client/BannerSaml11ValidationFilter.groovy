/* *****************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 ****************************************************************************** */

package net.hedtech.jasig.cas.client

import org.jasig.cas.client.validation.AbstractTicketValidationFilter
import org.jasig.cas.client.validation.Saml11TicketValidator
import org.jasig.cas.client.validation.TicketValidator

import javax.servlet.FilterConfig

/**
 * Saml11ValidationFilter of CAS client 3.1.8 does give an option to customise
 * the artifact and service parameters with 'ticket' and 'service' in place of
 * SAML's 'SAMLart' and 'TARGET'. This was helping in using SAML validation filter
 * for CAS authentication.
 *
 * CAS client 3.2.1 onwards, SAML11ValidationFilter claims full SAML 1.1. compliance
 * where it mandates artifact and service parameter names not to be customised.
 * So, options were:-
 * 1. Let the new cas client mandate the SAML 1.1 compliance using 'SAMLart' and 'TARGET'
 * by passing in the init parameter as 'SAMLart'.
 * 2. Take a copy of 3.1.8's Saml11ValidationFilter and introduced this as a validation
 * filter bean. This class is made for that. To allow fall back mechanism if a old CAS
 * server is being used which cannot support SAML 1.1 compliance.
 */
class BannerSaml11ValidationFilter extends AbstractTicketValidationFilter {
    public BannerSaml11ValidationFilter() {
        this.setArtifactParameterName("SAMLart");
        this.setServiceParameterName("TARGET");
    }

    protected final TicketValidator getTicketValidator(FilterConfig filterConfig) {
        Saml11TicketValidator validator = new Saml11TicketValidator(this.getPropertyFromInitParams(filterConfig, "casServerUrlPrefix", (String)null));
        validator.setRenew(this.parseBoolean(this.getPropertyFromInitParams(filterConfig, "renew", "false")));
        return validator;
    }
}

