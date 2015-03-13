//  The contents of this file are subject to the Mozilla Public License
//  Version 1.1 (the "License"); you may not use this file except in
//  compliance with the License. You may obtain a copy of the License
//  at http://www.mozilla.org/MPL/
//
//  Software distributed under the License is distributed on an "AS IS"
//  basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
//  the License for the specific language governing rights and
//  limitations under the License.
//
//  The Original Code is RabbitMQ.
//
//  The Initial Developer of the Original Code is GoPivotal, Inc.
//  Copyright (c) 2007-2014 GoPivotal, Inc.  All rights reserved.
//

package com.rabbitmq.client.impl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ContentHeader;

/**
 * Implementation of ContentHeader - specialized by autogenerated code in AMQP.java.
 */

public abstract class AMQContentHeader implements ContentHeader {
    /**
     * Private API - Called by {@link AMQChannel#handleFrame}. Parses the header frame.
     */
    private long bodySize; 
    
    protected AMQContentHeader() {
        this.bodySize = 0;
    }
    
    protected AMQContentHeader(DataInputStream in) throws IOException {
        in.readShort(); // weight not currently used
        this.bodySize = in.readLong();
    }
    
    public long getBodySize() { return bodySize; }
    

    private void writeTo(DataOutputStream out, long bodySize) throws IOException {
        out.writeShort(0); // weight - not currently used
        out.writeLong(bodySize);
        writePropertiesTo(new ContentHeaderPropertyWriter(out));
    }

    /**
     * Private API - Autogenerated writer for this header
     */
    public abstract void writePropertiesTo(ContentHeaderPropertyWriter writer) throws IOException;

    /** Public API - {@inheritDoc} */
    public void appendPropertyDebugStringTo(StringBuilder acc) {
        acc.append("(?)");
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("#contentHeader<").append(getClassName()).append(">");
        this.appendPropertyDebugStringTo(sb);
        return sb.toString();
    }

    /**
     * Private API - Called by {@link AMQCommand#transmit}
     */
    public Frame toFrame(int channelNumber, long bodySize) throws IOException {
        Frame frame = new Frame(AMQP.FRAME_HEADER, channelNumber);
        DataOutputStream bodyOut = frame.getOutputStream();
        bodyOut.writeShort(getClassId());
        writeTo(bodyOut, bodySize);
        return frame;
    }
    
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}