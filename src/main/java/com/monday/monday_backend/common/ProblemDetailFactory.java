package com.monday.monday_backend.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

public class ProblemDetailFactory {

    public static ProblemDetail of(HttpStatus status, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatus(status);
        pd.setTitle(title);
        pd.setDetail(detail);
        return pd;
    }

    public static ProblemDetail of(HttpStatus status, String title, String detail, String instancePath) {
        ProblemDetail pd = ProblemDetail.forStatus(status);
        pd.setTitle(title);
        pd.setDetail(detail);
        pd.setInstance(URI.create(instancePath));
        return pd;
    }

    public static ProblemDetail fromException(Exception ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setTitle("Unexpected Error");
        pd.setDetail(ex.getMessage());
        pd.setProperty("exceptionType", ex.getClass().getSimpleName());
        return pd;
    }

}
