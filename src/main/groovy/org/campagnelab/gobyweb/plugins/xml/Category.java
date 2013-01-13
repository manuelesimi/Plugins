/*
 * Copyright (c) 2011  by Cornell University and the Cornell Research
 * Foundation, Inc.  All Rights Reserved.
 *
 * Permission to use, copy, modify and distribute any part of GobyWeb web
 * application for next-generation sequencing data alignment and analysis,
 * officially docketed at Cornell as D-5061 ("WORK") and its associated
 * copyrights for educational, research and non-profit purposes, without
 * fee, and without a written agreement is hereby granted, provided that
 * the above copyright notice, this paragraph and the following three
 * paragraphs appear in all copies.
 *
 * Those desiring to incorporate WORK into commercial products or use WORK
 * and its associated copyrights for commercial purposes should contact the
 * Cornell Center for Technology Enterprise and Commercialization at
 * 395 Pine Tree Road, Suite 310, Ithaca, NY 14850;
 * email:cctecconnect@cornell.edu; Tel: 607-254-4698;
 * FAX: 607-254-5454 for a commercial license.
 *
 * IN NO EVENT SHALL THE CORNELL RESEARCH FOUNDATION, INC. AND CORNELL
 * UNIVERSITY BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL,
 * OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF
 * WORK AND ITS ASSOCIATED COPYRIGHTS, EVEN IF THE CORNELL RESEARCH FOUNDATION,
 * INC. AND CORNELL UNIVERSITY MAY HAVE BEEN ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 *
 * THE WORK PROVIDED HEREIN IS ON AN "AS IS" BASIS, AND THE CORNELL RESEARCH
 * FOUNDATION, INC. AND CORNELL UNIVERSITY HAVE NO OBLIGATION TO PROVIDE
 * MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.  THE CORNELL
 * RESEARCH FOUNDATION, INC. AND CORNELL UNIVERSITY MAKE NO REPRESENTATIONS AND
 * EXTEND NO WARRANTIES OF ANY KIND, EITHER IMPLIED OR EXPRESS, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY OR FITNESS FOR A
 * PARTICULAR PURPOSE, OR THAT THE USE OF WORK AND ITS ASSOCIATED COPYRIGHTS
 * WILL NOT INFRINGE ANY PATENT, TRADEMARK OR OTHER RIGHTS.
 */

package org.campagnelab.gobyweb.plugins.xml;

/**
 * Describes a category value. Used to describe individual categorical values of a CATEGORY type option.
 * @author Fabien Campagne
 *         Date: 10/9/11
 *         Time: 12:52 PM
 */
public class Category {
    /**
     * Identifier to use in shell scripts and config files to refer to this category value.
     * When a value for a category is stored in the database, it is the id that is stored.
     */
    public String id;

    /**
     * Name to display in the GobyWeb user interface.
     */
    public String name;

    /**
     * When a value is needed to generate command line options, etc. it is value that is used.
     * Value, itself, is never displayed to the user, stored in the database, etc.
     */
    public String value;

    /**
     * Text that describes the specific category to end-users.
     */
    public String help;

    /**
     * If exists, previously in the database we stored this value instead of id. During the conversion of
     * legacy to plugin, if a value equalling dbLegacyValue is encountered for a category, id will be stored
     * instead.
     */
    public String dbLegacyValue;

    public Category(String id, String name, String help) {
        this(id, name, help, (String) null);
    }

    public Category(String id, String name, String help, String dbLegacyValue) {
        this.id=id;
        this.name=name;
        this.help=help;
        this.dbLegacyValue=dbLegacyValue;
    }

    public Category() {
    }
}
