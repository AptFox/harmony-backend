package iterative.harmony.backend.exception

class AccessTokenMissingOrMalformedInRequestException() :
    IllegalArgumentException("Missing or malformed token in request")
