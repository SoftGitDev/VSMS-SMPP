package com.softtech.smpp.util;

import com.softtech.smpp.InvalidNumberOfDestinationsException;
import com.softtech.smpp.PDUStringException;
import com.softtech.smpp.bean.DestinationAddress;
import com.softtech.smpp.bean.OptionalParameter;
import com.softtech.smpp.bean.UnsuccessDelivery;

/**
 * This is utility to compose the PDU from parameter values to byte.
 * 
 * @author SUTHAR
 * 
 */
public interface PDUComposer {

    byte[] composeHeader(int commandId, int commandStatus, int sequenceNumber);

    // GENERAL BIND OPERATION
    byte[] bind(int commandId, int sequenceNumber, String systemId,
            String password, String systemType, byte interfaceVersion,
            byte addrTon, byte addrNpi, String addressRange)
            throws PDUStringException;

    byte[] bindResp(int commandId, int sequenceNumber, String systemId,
            OptionalParameter... optionalParameters) throws PDUStringException;

    byte[] bindResp(int commandId, int sequenceNumber, String systemId,
            byte scInterfaceVersion) throws PDUStringException;

    byte[] unbind(int sequenceNumber);

    byte[] unbindResp(int commandStatus, int sequenceNumber);

    byte[] outbind(int sequenceNumber, String systemId, String password)
            throws PDUStringException;

    // ENQUIRE_LINK OPERATION
    byte[] enquireLink(int sequenceNumber);

    byte[] enquireLinkResp(int sequenceNumber);

    // GENEICK_NACK OPERATION
    byte[] genericNack(int commandStatus, int sequenceNumber);

    // SUBMIT_SM OPERATION
    /**
     * Submit short message (submit_sm).
     * 
     * @param sequenceNumber
     * @param serviceType
     * @param sourceAddrTon
     * @param sourceAddrNpi
     * @param sourceAddr
     * @param destAddrTon
     * @param destAddrNpi
     * @param destinationAddr
     * @param esmClass
     * @param protocolId
     * @param priorityFlag
     * @param scheduleDeliveryTime
     * @param validityPeriod
     * @param registeredDelivery
     * @param replaceIfPresentFlag
     * @param dataCoding
     * @param smDefaultMsgId
     * @param shortMessage
     * @param optionalParameters
     * @return the composed byte values.
     * @throws PDUStringException
     */
    byte[] submitSm(int sequenceNumber, String serviceType, byte sourceAddrTon,
            byte sourceAddrNpi, String sourceAddr, byte destAddrTon,
            byte destAddrNpi, String destinationAddr, byte esmClass,
            byte protocolId, byte priorityFlag, String scheduleDeliveryTime,
            String validityPeriod, byte registeredDelivery,
            byte replaceIfPresentFlag, byte dataCoding, byte smDefaultMsgId,
            byte[] shortMessage, OptionalParameter... optionalParameters)
            throws PDUStringException;

    /**
     * Submit short message response (submit_sm_resp).
     * 
     * @param sequenceNumber
     * @param messageId
     * @return the composed byte values.
     * @throws PDUStringException
     */
    byte[] submitSmResp(int sequenceNumber, String messageId)
            throws PDUStringException;

    // QUERY_SM OPERATION
    byte[] querySm(int sequenceNumber, String messageId, byte sourceAddrTon,
            byte sourceAddrNpi, String sourceAddr) throws PDUStringException;

    byte[] querySmResp(int sequenceNumber, String messageId, String finalDate,
            byte messageState, byte errorCode) throws PDUStringException;

    // DELIVER_SM OPERATION
    byte[] deliverSm(int sequenceNumber, String serviceType,
            byte sourceAddrTon, byte sourceAddrNpi, String sourceAddr,
            byte destAddrTon, byte destAddrNpi, String destinationAddr,
            byte esmClass, byte protocolId, byte priorityFlag,
            byte registeredDelivery, byte dataCoding, byte[] shortMessage,
            OptionalParameter... optionalParameters) throws PDUStringException;

    byte[] deliverSmResp(int commandStatus, int sequenceNumber, String messageId);

    /**
     * Compose data short message (data_sm) PDU.
     * 
     * @param sequenceNumber
     * @param serviceType
     * @param sourceAddrTon
     * @param sourceAddrNpi
     * @param sourceAddr
     * @param destAddrTon
     * @param destAddrNpi
     * @param destinationAddr
     * @param esmClass
     * @param registeredDelivery
     * @param dataCoding
     * @param optionalParameters
     * @return
     * @throws PDUStringException
     */
    byte[] dataSm(int sequenceNumber, String serviceType, byte sourceAddrTon,
            byte sourceAddrNpi, String sourceAddr, byte destAddrTon,
            byte destAddrNpi, String destinationAddr, byte esmClass,
            byte registeredDelivery, byte dataCoding,
            OptionalParameter... optionalParameters) throws PDUStringException;

    /**
     * Compose data short message response (submit_sm_resp) PDU.
     * 
     * @param sequenceNumber is the sequence number.
     * @param messageId is the the message identifier.
     * @param optionalParameters is the optional parameter(s).
     * @return the composed byte values.
     * @throws PDUStringException
     */
    byte[] dataSmResp(int sequenceNumber, String messageId,
            OptionalParameter... optionalParameters) throws PDUStringException;

    /**
     * Compose cancel short message (cancel_sm) PDU.
     * 
     * @param sequenceNumber
     * @param serviceType
     * @param messageId
     * @param sourceAddrTon
     * @param sourceAddrNpi
     * @param sourceAddr
     * @param destAddrTon
     * @param destAddrNpi
     * @param destinationAddr
     * @return the composed byte values.
     * @throws PDUStringException
     */
    byte[] cancelSm(int sequenceNumber, String serviceType, String messageId,
            byte sourceAddrTon, byte sourceAddrNpi, String sourceAddr,
            byte destAddrTon, byte destAddrNpi, String destinationAddr)
            throws PDUStringException;

    /**
     * Compose cancel short message response (cancel_sm_resp) PDU.
     * 
     * @param sequenceNumber
     * @return the composed byte values.
     */
    byte[] cancelSmResp(int sequenceNumber);

    byte[] replaceSm(int sequenceNumber, String messageId, byte sourceAddrTon,
            byte sourceAddrNpi, String sourceAddr, String scheduleDeliveryTime,
            String validityPeriod, byte registeredDelivery,
            byte smDefaultMsgId, byte[] shortMessage) throws PDUStringException;

    byte[] replaceSmResp(int sequenceNumber);

    byte[] submitMulti(int sequenceNumber, String serviceType,
            byte sourceAddrTon, byte sourceAddrNpi, String sourceAddr,
            DestinationAddress[] destinationAddresses, byte esmClass, byte protocolId,
            byte priorityFlag, String scheduleDeliveryTime,
            String validityPeriod, byte registeredDelivery,
            byte replaceIfPresentFlag, byte dataCoding, byte smDefaultMsgId,
            byte[] shortMessage, OptionalParameter... optionalParameters)
            throws PDUStringException, InvalidNumberOfDestinationsException;

    byte[] submitMultiResp(int sequenceNumber, String messageId,
            UnsuccessDelivery... unsuccessDeliveries) throws PDUStringException;

    byte[] alertNotification(int sequenceNumber, byte sourceAddrTon,
            byte sourceAddrNpi, String sourceAddr, byte esmeAddrTon,
            byte esmeAddrNpi, String esmeAddr,
            OptionalParameter... optionalParameters) throws PDUStringException;
}