package iterative.harmony.backend.exception

import io.jsonwebtoken.JwtException

class TokenFingerprintMismatchException() :
    JwtException("Request fingerprint and token fingerprint do not match")
