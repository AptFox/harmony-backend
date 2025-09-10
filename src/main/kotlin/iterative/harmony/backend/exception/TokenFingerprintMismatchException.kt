package iterative.harmony.backend.exception

import io.jsonwebtoken.JwtException

class TokenFingerprintMismatchException() :
    JwtException("The current request fingerprint and supplied token fingerprint do not match")
