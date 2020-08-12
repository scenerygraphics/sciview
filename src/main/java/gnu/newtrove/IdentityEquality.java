package gnu.newtrove;

import gnu.newtrove.Equality;

class IdentityEquality<T> implements Equality<T> {
  public boolean equals(T o1, T o2) {
    return o1 == o2;
  }
}
