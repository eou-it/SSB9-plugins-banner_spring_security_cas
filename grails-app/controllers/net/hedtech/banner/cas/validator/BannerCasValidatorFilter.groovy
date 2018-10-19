package net.hedtech.banner.cas.validator


import org.jasig.cas.client.Protocol
import org.jasig.cas.client.configuration.ConfigurationKeys
import org.jasig.cas.client.validation.AbstractTicketValidationFilter
import org.jasig.cas.client.validation.TicketValidator

import javax.servlet.FilterConfig

class BannerCasValidatorFilter extends AbstractTicketValidationFilter {

    public BannerCasValidatorFilter() {
        super(Protocol.SAML11)
        //super(Protocol.CAS1)
    }


    protected final TicketValidator getTicketValidator(FilterConfig filterConfig) {
        super.setIgnoreInitConfiguration(true)
        super.setEncodeServiceUrl(false)
        println "In BannerSaml11CustomValidator.getTicketValidator ------------------------------------------"
        println "filterConfig.getInitParameter('casServerUrlPrefix') ${filterConfig.getInitParameter("casServerUrlPrefix")}"
        // final Cas20ServiceTicketValidator validator = new Cas20ServiceTicketValidator(filterConfig.getInitParameter("casServerUrlPrefix"))
        final BannerSaml11CustomValidator validator = new BannerSaml11CustomValidator(filterConfig.getInitParameter("casServerUrlPrefix"))
        validator.setRenew(false)
        //comment tolerance code for Cas20ServiceTicketValidator
        final long tolerance = getLong(ConfigurationKeys.TOLERANCE)
        validator.setTolerance(75000)
        return validator
    }
}

