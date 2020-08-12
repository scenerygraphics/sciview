package gnu.newtrove;


import gnu.newtrove.Equality;

class CanonicalEquality<T> implements Equality<T> {
  public boolean equals(T o1, T o2) {
    return o1 != null ? o1.equals(o2) : o2 == null;
  }
}
