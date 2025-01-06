package com.costin.travelify.controller;

import com.costin.travelify.dto.response_dto.ResponseDTO;
import com.costin.travelify.exceptions.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ExceptionHandlerController {
    @ExceptionHandler(BadUpdateDetailsProvidedException.class)
    public ResponseEntity<ResponseDTO> catchBadUpdateDetailsProvidedException(BadUpdateDetailsProvidedException e) {
        return new ResponseEntity<>(new ResponseDTO(e.getMessage()), HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(BadLocationException.class)
    public ResponseEntity<ResponseDTO> catchLocationTooFarException(BadLocationException e) {
        return new ResponseEntity<>(new ResponseDTO(e.getMessage()), HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(AlreadyExistingResourceException.class)
    public ResponseEntity<ResponseDTO> catchAlreadyExistingResourceException(AlreadyExistingResourceException e) {
        return new ResponseEntity<>(new ResponseDTO(e.getMessage()), HttpStatus.UNAUTHORIZED);
    }
    @ExceptionHandler(UnauthorizedOperationException.class)
    public ResponseEntity<ResponseDTO> catchUnauthorizedOperationException(UnauthorizedOperationException e) {
        return new ResponseEntity<>(new ResponseDTO(e.getMessage()), HttpStatus.UNAUTHORIZED);
    }
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ResponseDTO> catchUsernameNotFoundException(UsernameNotFoundException e) {
        return new ResponseEntity<>(new ResponseDTO(e.getMessage()), HttpStatus.NOT_FOUND);
    }
    @ExceptionHandler(BadQueryParametersException.class)
    public ResponseEntity<ResponseDTO> catchBadQueryParametersException(BadQueryParametersException e) {
        return new ResponseEntity<>(new ResponseDTO(e.getMessage()), HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(InsufficientPostDataException.class)
    public ResponseEntity<ResponseDTO> catchInsufficientPostDataException(InsufficientPostDataException e) {
        return new ResponseEntity<>(new ResponseDTO(e.getMessage()), HttpStatus.PARTIAL_CONTENT);
    }
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ResponseDTO> catchResourceNotFoundException(ResourceNotFoundException e) {
        return new ResponseEntity<>(new ResponseDTO(e.getMessage()), HttpStatus.NOT_FOUND);
    }
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ResponseDTO> catchUserNotFoundException(UserNotFoundException e) {
        return new ResponseEntity<>(new ResponseDTO(e.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(EmailAlreadyUsedException.class)
    public ResponseEntity<ResponseDTO> catchEmailAlreadyUsedException(EmailAlreadyUsedException e) {
        return new ResponseEntity<>(new ResponseDTO(e.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDTO> catchGeneralException(Exception e) {
        return new ResponseEntity<>(new ResponseDTO(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
}
