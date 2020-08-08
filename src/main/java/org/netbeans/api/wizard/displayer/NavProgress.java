package org.netbeans.api.wizard.displayer;

import org.netbeans.api.wizard.WizardDisplayer;
import org.netbeans.modules.wizard.NbBridge;
import org.netbeans.spi.wizard.ResultProgressHandle;
import org.netbeans.spi.wizard.Summary;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

/**
 * Show progress bar for deferred results, with a label showing percent done and progress bar.
 * 
 * <p>
 * <b><i><font color="red">This class is NOT AN API CLASS.  There is no
 * commitment that it will remain backward compatible or even exist in the
 * future.  The API of this library is in the packages <code>org.netbeans.api.wizard</code>
 * and <code>org.netbeans.spi.wizard</code></font></i></b>.

 * @author stanley@stanleyknutson.com
 */
public class NavProgress implements ResultProgressHandle
{
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(NavProgress.class);

    JProgressBar        progressBar = new JProgressBar();

    JLabel              lbl         = new JLabel();

    JLabel              busy        = new JLabel();

    WizardDisplayerImpl parent;

    String              failMessage = null;
    
    boolean             isUseBusy = false;
    
    Container   ipanel = null;
    
    boolean             isInitialized = false;
    
    /** isRunning is true until finished or failed is called */
    boolean             isRunning = true;
    
    NavProgress(WizardDisplayerImpl impl, boolean useBusy)
    {
        this.parent = impl;
        isUseBusy = useBusy;
    }
    
    public void addProgressComponents (Container panel)
    {
        panel.add(lbl);
        if (isUseBusy)
        {
            ensureBusyInitialized();
            panel.add(busy);
        }
        else
        {
            panel.add(progressBar);
        }
        isInitialized = true;
        ipanel = panel;
    }

    public void setProgress(final String description, final int currentStep, final int totalSteps)
    {
        Runnable r = new Runnable()
        {
            public void run()
            {
                lbl.setText(description == null ? " " : description); // NOI18N
                setProgress(currentStep, totalSteps);
            }
        };
        invoke(r);
    }

    public void setProgress(final int currentStep, final int totalSteps)
    {
        Runnable r = new Runnable()
        {
            public void run()
            {
                if (totalSteps == -1)
                {
                    progressBar.setIndeterminate(true);
                }
                else
                {
                    if (currentStep > totalSteps || currentStep < 0)
                    {
                        if (currentStep == -1 && totalSteps == -1)
                        {
                            return;
                        }
                        throw new IllegalArgumentException("Bad step values: " // NOI18N
                            + currentStep + " out of " + totalSteps); // NOI18N
                    }
                    progressBar.setIndeterminate(false);
                    progressBar.setMaximum(totalSteps);
                    progressBar.setValue(currentStep);
                }
                
                setUseBusy(false);
            }
        };
        invoke(r);
    }

    public void setBusy (final String description)
    {
        Runnable r = new Runnable()
        {
            public void run()
            {
                lbl.setText(description == null ? " " : description); // NOI18N

                progressBar.setIndeterminate(true);
                
                setUseBusy(true);
            }
        };
        invoke(r);
    }
    
    protected void setUseBusy (boolean useBusy)
    {
        if (isInitialized) 
        {
            if (useBusy && (! isUseBusy))
            {
                ipanel.remove(progressBar);
                ensureBusyInitialized();
                ipanel.add(busy);
                ipanel.invalidate();
            }
            else if ( !useBusy && isUseBusy)
            {
                ipanel.remove(busy);
                ipanel.add(progressBar);
                ipanel.invalidate();
            }
        }
        isUseBusy = useBusy; 
    }
    
    private void ensureBusyInitialized()
    {
        if (busy.getIcon() == null)
        {
            URL url = getClass().getResource("busy.gif");
            Icon icon = new ImageIcon(url);
            busy.setIcon(icon);
        }
    }
    
    private void invoke(Runnable r)
    {
        if (EventQueue.isDispatchThread())
        {
            r.run();
        }
        else
        {
            try
            {
                EventQueue.invokeAndWait(r);
            }
            catch (InvocationTargetException e)
            {
                logger.error("Error invoking operation " + e.getClass().getName() + " " + e.getMessage(), e);
            }
            catch (InterruptedException e)
            {
                logger.error("Error invoking operation " + e.getClass().getName() + " " + e.getMessage(), e);
            }
        }
    }

    public void finished(final Object o)
    {
        isRunning = false;
        Runnable r = new Runnable()
        {
            public void run()
            {
                if (o instanceof Summary)
                {
                    Summary summary = (Summary) o;
                    parent.handleSummary(summary);
                    parent.setWizardResult(summary.getResult());
                }
                else if (parent.getDeferredResult() != null)
                {
                    parent.setWizardResult(o);

                    // handle result based on which button was pushed
                    parent.getButtonManager().deferredResultFinished(o);
                }
            }
        };
        invoke(r);
    }

    public void failed(final String message, final boolean canGoBack)
    {
        failMessage = message;
        isRunning = false;

        Runnable r = new Runnable()
        {
            public void run()
            {
                // cheap word wrap
                JLabel comp = new JLabel("<html><body>" + message); // NOI18N
                comp.setBorder(new EmptyBorder(5, 5, 5, 5));
                parent.setCurrentWizardPanel(comp);
                parent.getTtlLabel().setText(
                                             NbBridge
                                                 .getString("org/netbeans/api/wizard/Bundle", // NOI18N
                                                            WizardDisplayer.class, "Failed")); // NOI18N
                NavButtonManager bm = parent.getButtonManager();
                bm.deferredResultFailed(canGoBack);
            }
        };
        invoke(r);
    }

    public boolean isRunning()
    {
        return isRunning;
    }
}
