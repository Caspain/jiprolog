/*
 * 23/04/2014
 *
 * Copyright (C) 1999-2014 Ugo Chirico - http://www.ugochirico.com
 *
 * This is free software; you can redistribute it and/or
 * modify it under the terms of the Affero GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Affero GNU General Public License for more details.
 *
 * You should have received a copy of the Affero GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package com.ugos.jiprolog.engine;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

class ZeroArityDefaultClausesDatabase extends JIPClausesDatabase
{
    protected final Vector<Clause> m_clausesVector;
    public ZeroArityDefaultClausesDatabase(final String strFunctName)
    {
        setFunctor(strFunctName, 0);
        m_clausesVector = new Vector<Clause>();
    }

    public void setAttributes(final String strAttribs)
    {
        // do nothing
    }

    public synchronized boolean addClauseAtFirst(final JIPClause clause)
    {
        m_clausesVector.add(0, (Clause)clause.getTerm());

        return true;
    }

    public synchronized boolean addClause(final JIPClause clause)
    {
        m_clausesVector.add((Clause)clause.getTerm());

        return true;
    }

    public synchronized boolean removeClause(final JIPClause clause)
    {
    	return m_clausesVector.removeElement(clause.getTerm());
    }

    public synchronized Enumeration clauses(JIPFunctor functor)
    {
    	if(!isDynamic() || getJIPEngine().isImmediateUpdateSemantics())
    		return m_clausesVector.elements();
    	else
        	return ((Vector<Clause>)m_clausesVector.clone()).elements();
    }

	@Override
	public synchronized Enumeration clauses()
	{
		if(!isDynamic() || getJIPEngine().isImmediateUpdateSemantics())
    		return m_clausesVector.elements();
    	else
        	return ((Vector<Clause>)m_clausesVector.clone()).elements();
	}
}