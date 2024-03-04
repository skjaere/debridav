package io.william.debrid.resource

import io.milton.http.http11.auth.DigestGenerator
import io.milton.http.http11.auth.DigestResponse
import io.milton.resource.DigestResource
import io.milton.resource.PropFindableResource


abstract class AbstractResource: DigestResource, PropFindableResource {
    override fun authenticate(user: String, requestedPassword: String): Any? {
        if (user == "user" && requestedPassword == "password") {
            return user
        }
        return null
    }

    override fun authenticate(digestRequest: DigestResponse): Any? {
        if (digestRequest.user == "user") {
            val gen = DigestGenerator()
            val actual = gen.generateDigest(digestRequest, "password")
            if (actual == digestRequest.responseDigest) {
                return digestRequest.user
            } else {
                //log.warn("that password is incorrect. Try 'password'")
            }
        } else {
            //log.warn("user not found: " + digestRequest.user + " - try 'user'")
        }
        return null
    }

}