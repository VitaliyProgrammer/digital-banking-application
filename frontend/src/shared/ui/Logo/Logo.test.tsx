import { render, screen } from '@testing-library/react';
import { Logo } from './Logo';

describe('Logo', () => {
  it('is announced by screen readers as the full bank name', () => {
    render(<Logo />);

    expect(screen.getByRole('img', { name: 'Taborovets Universal Bank' })).toBeInTheDocument();
  });

  it('spreads "Universal Bank" letter by letter, keeping the space between the words', () => {
    const { container } = render(<Logo variant="card" />);

    const letters = container.querySelectorAll('.subtitle > span');

    expect([...letters].map((letter) => letter.textContent).join('')).toBe('Universal Bank');
  });
});
