package com.softtech.smpp.session.state;

import java.io.IOException;

import com.softtech.smpp.bean.Command;
import com.softtech.smpp.extra.SessionState;
import com.softtech.smpp.session.BaseResponseHandler;
import com.softtech.smpp.session.ResponseHandler;

/**
 * This class is unbound state implementation of {@link SMPPSessionState}. All
 * this method is throw {@link IOException} since when the state is unbound we
 * should not give any positive response.
 *
 * @author SUTHAR
 * @version 1.0
 * @since 2.0
 *
 */
class SMPPSessionUnbound implements SMPPSessionState {

    private static final String INVALID_PROCESS_FOR_UNBOUND_SESSION = "Invalid process for unbound session state";

    public SessionState getSessionState() {
        return SessionState.UNBOUND;
    }

    public void processBindResp(Command pduHeader, byte[] pdu,
            ResponseHandler responseHandler) throws IOException {
        throw new IOException(INVALID_PROCESS_FOR_UNBOUND_SESSION);
    }

    public void processDeliverSm(Command pduHeader, byte[] pdu,
            ResponseHandler responseHandler) throws IOException {
        throw new IOException(INVALID_PROCESS_FOR_UNBOUND_SESSION);
    }

    public void processEnquireLink(Command pduHeader, byte[] pdu,
            BaseResponseHandler responseHandler) throws IOException {
        throw new IOException(INVALID_PROCESS_FOR_UNBOUND_SESSION);
    }

    public void processEnquireLinkResp(Command pduHeader, byte[] pdu,
            BaseResponseHandler responseHandler) throws IOException {
        throw new IOException(INVALID_PROCESS_FOR_UNBOUND_SESSION);
    }

    public void processGenericNack(Command pduHeader, byte[] pdu,
            BaseResponseHandler responseHandler) throws IOException {
        throw new IOException(INVALID_PROCESS_FOR_UNBOUND_SESSION);
    }

    public void processSubmitSmResp(Command pduHeader, byte[] pdu,
            ResponseHandler responseHandler) throws IOException {
        throw new IOException(INVALID_PROCESS_FOR_UNBOUND_SESSION);
    }

    public void processSubmitMultiResp(Command pduHeader, byte[] pdu,
            ResponseHandler responseHandler) throws IOException {
        throw new IOException(INVALID_PROCESS_FOR_UNBOUND_SESSION);
    }

    public void processUnbind(Command pduHeader, byte[] pdu,
            BaseResponseHandler responseHandler) throws IOException {
        throw new IOException(INVALID_PROCESS_FOR_UNBOUND_SESSION);
    }

    public void processUnbindResp(Command pduHeader, byte[] pdu,
            BaseResponseHandler responseHandler) throws IOException {
        throw new IOException(INVALID_PROCESS_FOR_UNBOUND_SESSION);
    }

    public void processUnknownCid(Command pduHeader, byte[] pdu,
            BaseResponseHandler responseHandler) throws IOException {
        throw new IOException(INVALID_PROCESS_FOR_UNBOUND_SESSION);
    }

    public void processQuerySmResp(Command pduHeader, byte[] pdu,
            ResponseHandler responseHandler) throws IOException {
        throw new IOException(INVALID_PROCESS_FOR_UNBOUND_SESSION);
    }

    public void processDataSm(Command pduHeader, byte[] pdu,
            BaseResponseHandler responseHandler) throws IOException {
        throw new IOException(INVALID_PROCESS_FOR_UNBOUND_SESSION);
    }

    public void processDataSmResp(Command pduHeader, byte[] pdu,
            BaseResponseHandler responseHandler) throws IOException {
        throw new IOException(INVALID_PROCESS_FOR_UNBOUND_SESSION);
    }

    public void processCancelSmResp(Command pduHeader, byte[] pdu,
            ResponseHandler responseHandler) throws IOException {
        throw new IOException(INVALID_PROCESS_FOR_UNBOUND_SESSION);
    }

    public void processReplaceSmResp(Command pduHeader, byte[] pdu,
            ResponseHandler responseHandler) throws IOException {
        throw new IOException(INVALID_PROCESS_FOR_UNBOUND_SESSION);
    }

    public void processAlertNotification(Command pduHeader, byte[] pdu,
            ResponseHandler responseHandler) {
    }
}
