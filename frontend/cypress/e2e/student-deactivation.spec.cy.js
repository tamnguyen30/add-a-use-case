/* global describe, beforeEach, it, cy */

const student = {
  id: 4,
  username: 'j.smith@abc.edu',
  enabled: true,
  firstName: 'John',
  lastName: 'Smith',
  email: 'j.smith@abc.edu',
  teamName: 'Team1'
}

describe('Student deactivation warning', () => {
  beforeEach(() => {
    cy.login('b.gates@abc.edu', '123456')
    cy.intercept('POST', '**/students/search*', {
      body: {
        flag: true,
        code: 200,
        message: 'Find students successfully',
        data: {
          content: [student],
          pageable: {},
          totalElements: 1,
          totalPages: 1,
          last: true,
          size: 50,
          number: 0,
          sort: {},
          numberOfElements: 1,
          first: true,
          empty: false
        }
      }
    }).as('students')
    cy.intercept('GET', '**/students/4', {
      body: { flag: true, code: 200, message: 'Find student successfully', data: student }
    }).as('student')
    cy.intercept('POST', '**/students/4/deactivate', {
      body: { flag: true, code: 200, message: 'Student deactivated successfully', data: student }
    }).as('deactivate')
    cy.visit('/students')
    cy.wait('@students')
    cy.get('[data-cy=student-edit]').click()
    cy.wait('@student')
  })

  it('cancels without sending a request and explains the effects', () => {
    cy.get('[data-cy=student-deactivate-open]').click()

    cy.get('[data-cy=student-deactivation-consequences]')
      .should('contain', 'team membership')
      .and('contain', 'previously submitted work')
      .and('contain', 'New peer evaluations cannot target')
      .and('contain', 'reports will continue to include them for active weeks')
      .and('contain', 'next protected request will be denied')

    cy.get('[data-cy=student-deactivate-cancel]').click()

    cy.get('[data-cy=student-deactivation-warning]').should('not.be.visible')
    cy.get('[data-cy=student-deactivate-open]').should('be.visible')
    cy.get('@deactivate.all').should('have.length', 0)
  })
})
