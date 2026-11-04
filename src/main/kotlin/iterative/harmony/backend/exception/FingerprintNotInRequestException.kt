package iterative.harmony.backend.exception

import io.jsonwebtoken.JwtException

class FingerprintNotInRequestException() : JwtException("No fingerprint cookie in request")
