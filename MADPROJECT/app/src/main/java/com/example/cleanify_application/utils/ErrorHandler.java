package com.example.cleanify_application.utils;

import android.content.Context;
import android.widget.Toast;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import okhttp3.Response;

/**
 * Utility class for error handling with user-friendly messages
 */
public class ErrorHandler {

    private ErrorHandler() {
        // Private constructor to prevent instantiation
    }

    /**
     * Handles network errors and shows appropriate user-friendly message
     * @param context Context for showing toast
     * @param e Exception that occurred
     */
    public static void handleNetworkError(Context context, Exception e) {
        String message = getUserFriendlyErrorMessage(e);
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Handles HTTP response errors
     * @param context Context for showing toast
     * @param response HTTP response
     * @param responseBody Response body for additional error info
     */
    public static void handleResponseError(Context context, Response response, String responseBody) {
        String message;
        
        if (responseBody != null && !responseBody.isEmpty()) {
            message = JsonParserUtils.extractErrorMessage(responseBody);
        } else {
            message = getHttpErrorMessage(response.code());
        }
        
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Gets user-friendly error message from exception
     * @param e Exception that occurred
     * @return User-friendly error message
     */
    public static String getUserFriendlyErrorMessage(Exception e) {
        if (e == null) {
            return "An unknown error occurred";
        }

        if (e instanceof SocketTimeoutException) {
            return "Connection timeout. Please check your internet connection and try again.";
        }

        if (e instanceof UnknownHostException) {
            return "No internet connection. Please check your network settings.";
        }

        if (e instanceof IOException) {
            return "Network error. Please check your internet connection.";
        }

        return "An error occurred: " + e.getMessage();
    }

    /**
     * Gets user-friendly message for HTTP error codes
     * @param code HTTP status code
     * @return User-friendly error message
     */
    public static String getHttpErrorMessage(int code) {
        switch (code) {
            case 400:
                return "Invalid request. Please check your input.";
            case 401:
                return "Authentication failed. Please log in again.";
            case 403:
                return "You don't have permission to perform this action.";
            case 404:
                return "The requested resource was not found.";
            case 409:
                return "This record already exists.";
            case 422:
                return "Invalid data provided. Please check your input.";
            case 429:
                return "Too many requests. Please wait a moment and try again.";
            case 500:
                return "Server error. Please try again later.";
            case 502:
            case 503:
            case 504:
                return "Service unavailable. Please try again later.";
            default:
                return "An error occurred (Error code: " + code + ")";
        }
    }

    /**
     * Shows a generic error message
     * @param context Context for showing toast
     * @param message Error message to show
     */
    public static void showError(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Shows a success message
     * @param context Context for showing toast
     * @param message Success message to show
     */
    public static void showSuccess(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Logs error for debugging
     * @param tag Log tag
     * @param message Error message
     * @param e Exception
     */
    public static void logError(String tag, String message, Exception e) {
        android.util.Log.e(tag, message, e);
    }
}
