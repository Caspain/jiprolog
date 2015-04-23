/*
 * 23/04/2014
 *
 * Copyright (C) 1999-2014 Ugo Chirico
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

import java.util.Hashtable;
import java.io.*;

class Clause extends ConsCell
{
    final static long serialVersionUID = 300000002L;

    private String  m_strModuleName;
    private boolean m_bExported = false;
    private String  m_strFileName = null;//"none";
    private int     m_nPosition = 0;
    private int     m_nLineNumber = 0;

    private static JIPEngine s_engine = null;
    private static Functor s_translateQuery = null;
    private static ConsCell s_translateParams = null;

    Clause(String strModuleName, final Functor lhs, final ConsCell rhs)
    {
        super(lhs, rhs);
        m_strModuleName = strModuleName;
    }

    private Clause(final ConsCell cell, String strModuleName)
    {
        this(strModuleName, (Functor)cell.m_head, (ConsCell)cell.m_tail);
    }

    final void setModuleName(final String strModuleName)
    {
        m_strModuleName = strModuleName;
    }

    final String getModuleName()
    {
        return m_strModuleName;
    }

    final void setExported()
    {
        m_bExported = true;
    }

    final void setFileName(String strFileName)
    {
        m_strFileName = strFileName;
    }

    final void setPosition(int nPos)
    {
        m_nPosition = nPos;
    }

    final void setLineNumber(int nLineNumber)
    {
        m_nLineNumber = nLineNumber;
    }

    final String getFileName()
    {
        return m_strFileName;
    }

    final int getPosition()
    {
        return m_nPosition;
    }

    final int getLineNumber()
    {
        return m_nLineNumber;
    }

    final boolean isExported()
    {
        return m_bExported;
    }


    public final PrologObject copy(final boolean flat, final Hashtable<Variable, PrologObject> varTable)
    {
        final Clause clause = new Clause((ConsCell)super.copy(flat, varTable), m_strModuleName);
        clause.m_bExported = m_bExported;
        clause.m_strFileName = m_strFileName;
        clause.m_nLineNumber = m_nLineNumber;
        clause.m_nPosition = m_nPosition;
        return clause;
    }

    static final Clause getClause(PrologObject pred)
    {
        return getClause(pred, GlobalDB.USER_MODULE);
    }

    static final Clause getClause(PrologObject pred, String strModuleName)
    {
//    	boolean isSystem = strModuleName.equals("$system");
//    	if(!isSystem)
//    	{
	        if(pred instanceof Variable)
	            pred = ((Variable)pred).getObject();

	        if(pred == null)
	        	throw new JIPParameterUnboundedException();

	        if(pred instanceof Clause)
	            return (Clause)pred;

	        if(pred instanceof Atom)
	            pred = new Functor(((Atom)pred).getName() + "/0", null);

	        if(!(pred instanceof Functor))
	        	throw new JIPTypeException(JIPTypeException.CALLABLE, pred);
//    	}

        Functor func = (Functor)pred;

        Clause clause;
        ConsCell params;

        if(func.getName().equals(":-/2"))
        {
            // estrae la clausola
            params = func.getParams();

            PrologObject lhs = BuiltIn.getRealTerm(params.getHead());
            PrologObject rhs = BuiltIn.getRealTerm(params.getTail());

            if(lhs == null || rhs == null)
            	throw new JIPParameterUnboundedException();

            // verifica se lhs ha la specifica del modulo
            if((lhs instanceof Functor) && ((Functor)lhs).getName().equals(":/2"))
            {
                strModuleName = ((Atom)((Functor)lhs).getParams().getHead()).getName();
                lhs = BuiltIn.getRealTerm(((ConsCell)((Functor)lhs).getParams().getTail()).getHead());
            }

            if(lhs instanceof Atom)
            {
                lhs = new Functor(((Atom)lhs).getName() + "/0", null);
            }

            if(!strModuleName.equals("$system"))
            {
	            if(!(lhs instanceof Functor))
	            	throw new JIPTypeException(JIPTypeException.CALLABLE, lhs);

				if(!(rhs instanceof ConsCell))
	            	throw new JIPTypeException(JIPTypeException.CALLABLE, rhs);

				checkForCallable((ConsCell)rhs);
        	}

            clause = new Clause(strModuleName, (Functor)lhs, (ConsCell)rhs);
        }
        else if(func.getName().equals("-->/2"))
        {
            PrologObject translated;
            // chiama il prolog per la translation
            if(s_engine == null)
                s_engine = JIPEngine.getDefaultEngine();

            if(s_translateQuery == null)
            {
                final PrologParser parser = new PrologParser(new ParserReader(new InputStreamReader(new ByteArrayInputStream("translate(X, Y)".getBytes()))), new OperatorManager(),"internal");
                try
                {
                    final Functor funct = ((Functor)parser.parseNext());
                    s_translateParams = funct.getParams();
                    s_translateQuery = new Functor(":/2", new ConsCell(Atom.createAtom(GlobalDB.KERNEL_MODULE), new ConsCell(funct, null)));
                }
                catch(JIPSyntaxErrorException ex)
                {
                    throw new JIPRuntimeException(ex.toString());
                }
            }
            Variable vTranslated = new Variable("Y");
            s_translateParams.setHead(func);
            ((ConsCell)s_translateParams.getTail()).setHead(vTranslated);

            WAM wam = new WAM(s_engine);

            if(wam.query(new ConsCell(s_translateQuery, null)))
            {
                wam.closeQuery();

                // estrae la collection di clausole
                translated = BuiltIn.getRealTerm(vTranslated);

                // chiama getClause e ritorna
                clause = getClause(translated.copy(false), strModuleName);

                wam.closeQuery();

                return clause;
            }
            else
            {
                throw new JIPTypeException(JIPTypeException.CALLABLE, pred);
            }
        }
        else if(func.getName().equals(":/2"))
        {
            // solo funtore con specifica di modulo
            // il body � vuoto
            strModuleName = ((Atom)(func).getParams().getHead()).getName();
            PrologObject lhs = BuiltIn.getRealTerm(((ConsCell)(func).getParams().getTail()).getHead());

            if(lhs instanceof Atom)
            {
                lhs = new Functor(((Atom)lhs).getName() + "/0", null);
            }

            clause = new Clause(strModuleName, (Functor)lhs, null);

        }
        else
        {
            // solo funtore in modulo user
            clause = new Clause(strModuleName, func, null);

        }

//        clause.setModuleName(strModuleName);
//        System.out.println("clause: " + clause );
//        System.out.println("module: " + clause.getModuleName() );
//
        return clause;
    }

    private static void checkForCallable(ConsCell rhs)
    {
    	// check rhs
		PrologObject head = ((ConsCell)rhs).m_head;
		PrologObject tail = ((ConsCell)rhs).m_tail;

		if(head instanceof Expression)
		{
			throw new JIPTypeException(JIPTypeException.CALLABLE, head);
		}
		else if(head instanceof ConsCell && !(head instanceof List) && !(head instanceof Functor))
		{
			checkForCallable((ConsCell)head);
		}
//    	else if(head instanceof PString)
//    		throw new JIPTypeException(JIPTypeException.CALLABLE, rhs);
    	else if(head instanceof Variable)
    		((ConsCell)rhs).m_head = new Functor("call/1", new ConsCell(head, null));

		while(tail != null)
        {
			if(!(tail instanceof ConsCell))
				throw new JIPTypeException(JIPTypeException.CALLABLE, rhs);

			head = ((ConsCell)tail).m_head;

			if(head instanceof Expression)
			{
				throw new JIPTypeException(JIPTypeException.CALLABLE, rhs);
			}
			else if(head instanceof ConsCell && !(head instanceof List) && !(head instanceof Functor))
			{
				checkForCallable((ConsCell)head);
			}
//    		else if(head instanceof PString)
//    			throw new JIPTypeException(JIPTypeException.CALLABLE, rhs);
//    		else if(head instanceof Variable)
//    			((ConsCell)tail).m_head = new Functor("call/1", new ConsCell(head, null));

			tail = ((ConsCell)tail).m_tail;
        }
    }
}
