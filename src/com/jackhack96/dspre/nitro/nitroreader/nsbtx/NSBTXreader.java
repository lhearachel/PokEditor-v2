/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jackhack96.dspre.nitro.nitroreader.nsbtx;

import com.jackhack96.dspre.nitro.nitroreader.shared.ByteReader;
import com.jackhack96.dspre.nitro.nitroreader.shared.G3Dfile;
import com.jackhack96.dspre.nitro.nitroreader.shared.G3Dheader;
import com.jackhack96.dspre.nitro.nitroreader.shared.G3Dreader;

/**
 *
 * @author Usuario
 */
public class NSBTXreader implements G3Dreader{
    private final ByteReader data;

    public NSBTXreader(ByteReader data) {
        this.data = data;
    }

    @Override
    public G3Dfile readFile() {
        
        G3Dheader header = new G3Dheader(data);
        data.setIndex(header.getOffsets()[0]);
        
        TexPlttSet TEX0 = new TexPlttSet(data);
        
        return new NSBTX(TEX0);
    }
}
