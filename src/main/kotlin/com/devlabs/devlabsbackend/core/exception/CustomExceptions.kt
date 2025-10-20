package com.devlabs.devlabsbackend.core.exception

class NotFoundException(message: String) : RuntimeException(message)

class ForbiddenException(message: String) : RuntimeException(message)

class BadRequestException(message: String) : RuntimeException(message)

class UnauthorizedException(message: String) : RuntimeException(message)

class ConflictException(message: String) : RuntimeException(message)
