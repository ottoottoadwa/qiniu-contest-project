package com.prreview.domain.model.feedback;

/** User verdict on a risk item — drives the feedback calibration loop. */
public enum FeedbackVerdict {
    /** The risk item was valid and the user acted on it. */
    ACCEPTED,
    /** The risk item was a false positive. */
    FALSE_POSITIVE,
    /** The user acknowledged but chose not to act. */
    IGNORED
}
