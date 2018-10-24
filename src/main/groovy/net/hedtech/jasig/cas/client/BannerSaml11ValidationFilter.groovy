/* *****************************************************************************
 Copyright 2015-2018 Ellucian Company L.P. and its affiliates.
 ****************************************************************************** */

package net.hedtech.jasig.cas.client

import groovy.util.logging.Slf4j
import org.jasig.cas.client.Protocol
import org.jasig.cas.client.configuration.ConfigurationKeys
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
@Slf4j
class BannerSaml11ValidationFilter extends AbstractTicketValidationFilter {
    public BannerSaml11ValidationFilter() {
        super(Protocol.SAML11)
    }


    protected final TicketValidator getTicketValidator(final FilterConfig filterConfig) {
        final BannerSaml11CustomValidator validator = new BannerSaml11CustomValidator(getString(ConfigurationKeys.CAS_SERVER_URL_PREFIX))
        final long tolerance = getLong(ConfigurationKeys.TOLERANCE)
        validator.setTolerance(tolerance)
        validator.setRenew(true)
        validator.setEncoding(getString(ConfigurationKeys.ENCODING))
        return validator
    }
}


