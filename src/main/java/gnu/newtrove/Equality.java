package gnu.newtrove;

import gnu.newtrove.CanonicalEquality;
import gnu.newtrove.IdentityEquality;

public interface Equality<T> {
  Equality CANONICAL = new CanonicalEquality();
  Equality IDENTITY = new IdentityEquality();
  boolean equals(T o1, T o2);
}
