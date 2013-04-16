package org.campagnelab.gobyweb.clustergateway.data;

import java.util.HashSet;
import java.util.Set;

/**
 * A Job with input parameters.
 *
 * @author manuele
 */
public abstract class ParametrizedJob extends Job {

    Set<InputParameter> parameters = new HashSet<InputParameter>();

    /**
     * Adds a new parameter to the job
     * @param parameter
     * @throws InvalidParameterException if the parameter is not valid
     */
    public void addParameter(InputParameter parameter) throws InvalidParameterException{
      if (!(parameter.getValues().size()>0))
          throw new InvalidParameterException(String.format("Parameter %s has no value(s) " + parameter.toString()));
      else if (! validateParameter(parameter))
          throw new InvalidParameterException(String.format("Invalid parameter %s ", parameter.toString()));
      else
          parameters.add(parameter);
    }

    /**
     * Adds new parameters to the job.
     * @param parameters
     * @throws InvalidParameterException if any f the parameters is not valid
     */
    public void addParameters(Set<InputParameter> parameters) throws InvalidParameterException{
        for (InputParameter parameter : parameters)
            this.addParameter(parameter);

    }

    /**
     * Validates the parameter.
     *
     * @param parameter
     * @return true if the parameter was accepted, false otherwise
     */
    protected abstract boolean validateParameter(InputParameter parameter);

    public static class InvalidParameterException extends Exception {

        public InvalidParameterException(String message) {
            super(message);
        }
    }
}
